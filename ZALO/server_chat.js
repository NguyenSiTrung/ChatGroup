//setup express
var express = require('express');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var mongodb = require('mongodb');
var fs      = require("fs");

var MongoClient = mongodb.MongoClient;
var url = 'mongodb://localhost:27017/chatapp';

var roles = {
  sender  : "",
  receiver    : ""  
};

// Routing
app.use(express.static(__dirname + '/public'));
 
// Chatroom
 
var numUsers = 0;

MongoClient.connect(url, function (err, db) {
  if (err) {
    console.log('Unable to connect to the mongoDB server. Error:', err);
  } else {
    //HURRAY!! We are connected. :)
    console.log('Connection established to', url);
    collection = db.collection('users_login');
	collectionMess = db.collection('addMessage');
  }
});

http.listen(3000, function(){
	console.log('listening on *:3000');
});

// Chatroom
 
var numUsers = 0; 
 
io.on('connection', function (socket) {
	
	
	
	socket.on("save avatar", function(data, fileName){
		var savedFilename = "/avatar/"+fileName;
		fs.writeFile(__dirname+"/public"+savedFilename, data, 'base64', function(err) {
			if (err !== null){
				console.log(err);
			}else{
				console.log("save avatar success!");
			}	
		});
	});
		
	socket.on('register', function (name, password, email ) {
		console.log(name + "register");

		var user = {name: name, password: password, email: email };
		
		collection.insert(user, function (err, result) {
		  if (err) {
			 console.log(err);
			 socket.emit('register', false);
		  } else {
			console.log('Inserted new user ok');
			socket.emit('register', true);
		  }
		});
	});
	
	//login
	socket.on('login', function(email, password){
		console.log(email + "login");

		var cursor = collection.find({"email":email});
		cursor.each(function (err, doc) {
			if (err) {
				console.log(err);
				socket.emit('login', false);
			} else {
				if(doc !== null){
					if(doc.password == password){
						socket.username = doc.name;
						socket.email=email;
						socket.emit('login', true, {
							username: socket.username,
							numUsers: numUsers
						});
						++numUsers;
						//get avatar
						var savedFilename = "/avatar/"+email+".bin";
						fs.readFile(__dirname+"/public"+savedFilename, 'base64', function(err, data) {
							if (err !== null){
								console.log(err);
							}else{
								socket.avatar= data;
							}	
						});
						
						// echo globally (all clients) that a person has connected
						socket.broadcast.emit('user joined', {
							username: socket.username,
							numUsers: numUsers
						})
					}else{
						socket.emit('login', false);
					}
				}else{
					socket.emit('login', false);
				}
			}
		});
	
		//get history
		socket.on("get history", function(){
			console.log("getting history");
			var listMess = [];
			var cursor = collectionMess.find();
				cursor.nextObject(function(err, doc){
					var savedFilename = "/avatar/"+doc.email+".bin";
					fs.readFile(__dirname+"/public"+savedFilename, 'base64', function(err, mAvatar) {
						if (err !== null){
							console.log(err);
						}else{
							listMess.push({username: doc.username, time: doc.time, data: doc.data, avatar: mAvatar});
							socket.emit('get history',listMess);
						}	
					});
				
				});
			console.log('DONE');
		});
	
		//add user to room
		socket.on('add user',function(){
			username = socket.username;
			console.log(username + "login");
			++numUsers;
			addedUser = true;
			socket.emit('login', {
				numUsers: numUsers
			});
			// echo globally (all clients) that a person has connected
			socket.broadcast.emit('user joined', {
				username: socket.username,
				numUsers: numUsers
			});
			
		});
		
		// when the client emits 'new message', this listens and executes
		socket.on('new message', function (time, data) {
			// we tell the client to execute 'new message'
			var newMess = {username: socket.username, time: time, data: data
						, email: socket.email };

			collectionMess.insert(newMess, function (err, result) {
				if (err) {
					console.log(err);
				} else {
					console.log('Save message ok');
					socket.broadcast.emit('new message', {
						username: socket.username,
						message: data,
						timeMess: time,
						avatar: socket.avatar
					});
				}
			});
		});


		// when the client emits 'typing', we broadcast it to others
		socket.on('typing', function () {
			socket.broadcast.emit('typing', {
				username: socket.username
			});
		});

		// when the client emits 'stop typing', we broadcast it to others
		socket.on('stop typing', function () {
			socket.broadcast.emit('stop typing', {
				username: socket.username
			});
		});

		// when the user disconnects.. perform this
		socket.on('disconnect', function () {
		
			--numUsers;

			// echo globally that this client has left
			socket.broadcast.emit('user left', {
				username: socket.username,
				numUsers: numUsers
			});
		});
		
		socket.on("sendPhoto", function(data, fileName, mTime){
			var savedFilename = "/upload/"+fileName;
			fs.writeFile(__dirname+"/public"+savedFilename, data, 'utf-8', function(err) {
				if (err !== null){
					console.log(err);
				}else{
					socket.broadcast.emit('file sent', {
						path: savedFilename,
						name: fileName,
						time: mTime
					});
					
					console.log("Send file success!");
				}	
			});
		});
		
		socket.on("receivePhoto", function(path, filename){
			var savedFilename = path;
			fs.readFile(__dirname+"/public"+savedFilename, 'utf-8', function(err, data) {
				if (err !== null){
					console.log(err);
				}else{
					console.log(data);
					socket.emit('receivePhotos', {
						fileData: data,
						name: filename
					});
				}	
			});
		});
		
		socket.on("get avatar", function(email){
			var savedFilename = "/avatar/"+email+".bin";
			fs.readFile(__dirname+"/public"+savedFilename, 'base64', function(err, data) {
				if (err !== null){
					console.log(err);
				}else{
					console.log("get avatar success");
					socket.emit('get avatar', {
						avatar: data
					});
				}	
			});
		});
		
		socket.on("get avatar history", function(idList, email){
			var savedFilename = "/avatar/"+email+".bin";
			fs.readFile(__dirname+"/public"+savedFilename, 'base64', function(err, data) {
				if (err !== null){
					console.log(err);
				}else{
					console.log("get avatar history success");
					socket.emit('get avatar history', {
						id: idList,
						avatar: data
					});
				}	
			});
		});
		
	});

});

function randomString(length)
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    for( var i=0; i < length; i++ )
        text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
}

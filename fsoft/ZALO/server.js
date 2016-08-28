var express = require('express');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var mongodb = require('mongodb');



var MongoClient = mongodb.MongoClient;
var url = 'mongodb://localhost:27017/chatapp';

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
     
  }
});


 
app.get('/', function (req, res){
  res.sendfile('index.html');
});
 
 
io.on('connection', function (socket) {
	var addedUser = false;
	socket.on('login', function (email, password) {
		console.log(email + "login");

		var cursor = collection.find({email:email});
		cursor.each(function (err, doc) {
			if (err) {
				console.log(err);
			} else {
				if(doc != null){
					if(doc.password == password){
						if (addedUser) return;
						// we store the email in the socket session for this client
						socket.email = email;
						++numUsers;
						addedUser = true;
						
						socket.emit('login', {
							numUsers: numUsers
						});
						// echo globally (all clients) that a person has connected
						socket.broadcast.emit('user joined', {
							email: socket.email,
							numUsers: numUsers
						});
					}
				}
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
	
	// when the client emits 'new message', this listens and executes
	socket.on('new message', function (data) {
		// we tell the client to execute 'new message'
		socket.broadcast.emit('new message', {
			email: socket.email,
			message: data
		});
	});


	// when the client emits 'typing', we broadcast it to others
	socket.on('typing', function () {
		socket.broadcast.emit('typing', {
			email: socket.email
		});
	});

	// when the client emits 'stop typing', we broadcast it to others
	socket.on('stop typing', function () {
		socket.broadcast.emit('stop typing', {
			email: socket.email
		});
	});

	// when the user disconnects.. perform this
	socket.on('disconnect', function () {
		if (addedUser) {
			--numUsers;

			// echo globally that this client has left
			socket.broadcast.emit('user left', {
				email: socket.email,
				numUsers: numUsers
			});
		}
	});
});


 
http.listen(3000, function(){
	console.log('listening on *:3000');
});
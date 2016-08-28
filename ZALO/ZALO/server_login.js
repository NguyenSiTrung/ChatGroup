var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var mongodb = require('mongodb');

var MongoClient = mongodb.MongoClient;
var url = 'mongodb://localhost:27017/test';


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
  socket.on('login', function (email, password) {
    console.log(email + "login");

    var cursor = collection.find({email:email});
    cursor.each(function (err, doc) {
      if (err) {
        console.log(err);
        socket.emit('login', false);
      } else {
         if(doc != null){
         	if(doc.password == password){
         		socket.emit('login', true);
         	}else{
         		socket.emit('login', false);
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


});
 
http.listen(3000, function(){
  console.log('listening on *:3000');
});
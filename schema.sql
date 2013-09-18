DROP TABLE Hobby;
DROP TABLE EmailConfirmation;
DROP TABLE FriendRequest;
DROP TABLE Session;
DROP TABLE Admin;
DROP TABLE Friend;
DROP TABLE User;
DROP TABLE UserHobby;


CREATE TABLE User
(
ID int NOT NULL PRIMARY KEY,
Name Varchar(255) NOT NULL,
Address Varchar(255),
Email Varchar(255) NOT NULL,
Password Varchar(255) NOT NULL,
UNIQUE KEY `emailUnique` (Email)
);

CREATE TABLE Friend
(
User1 int NOT NULL FOREIGN KEY REFERENCES User(ID),
User2 int NOT NULL FOREIGN KEY REFERENCES User(ID),
Status Enum('Relationship', 'Friend') NOT NULL DEFAULT 'Friend',
PRIMARY KEY (User1, User2)
);

CREATE TABLE Admin
(
	User int NOT NULL FOREIGN KEY REFERENCES User(ID)
);

CREATE TABLE Session
(
	SessionKey varchar(36) NOT NULL,
	User int NOT NULL FOREIGN KEY REFERENCES User(ID) PRIMARY KEY
);

CREATE TABLE FriendRequest
(
	FromUser int NOT NULL FOREIGN KEY REFERENCES User(ID),
	ToUser int NOT NULL FOREIGN KEY REFERENCES User(ID),
	Status Enum('Relationship', 'Friend') NOT NULL DEFAULT 'Friend',
	PRIMARY KEY (FromUser, ToUser)	
);

CREATE TABLE EmailConfirmation
(
	GUID varchar(36) NOT NULL,
	User int NOT NULL FOREIGN KEY REFERENCES User(ID) PRIMARY KEY
);

CREATE TABLE Hobby
(
	ID int NOT NULL PRIMARY KEY,
	Name varchar(255) NOT NULL
);

CREATE TABLE UserHobby
(
	User int NOT NULL FOREIGN KEY REFERENCES User(ID),
	Hobby int NOT NULL FOREIGN KEY REFERENCES Hobby(ID),
	PRIMARY KEY (User, Hobby)
);
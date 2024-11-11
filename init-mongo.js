// Connect to the desired database
db = db.getSiblingDB('taskManagement');

// Create a collection
db.createCollection("users");
db.createCollection("tasks");

// Insert an admin user into the 'users' collection
db.users.insertOne({
    username: "admin",
    email: "admin@example.com",
    password: "$2y$12$vZdxNmBdLBsw82C4wyoEXuZ8qsvU6zM9p8BjM.EExbeqZ5VAOT882", // Replace with the hashed password from the bcrypt script
    role: "ADMIN",
    permission: "ALL",
    createdAt: new Date(),
    updatedAt: new Date()
});

// Create roles
db.createRole({
    role: "readWriteAccess",
    privileges: [
        { resource: { db: "taskManagement", collection: "tasks" }, actions: ["find", "insert", "update", "remove"] }
    ],
    roles: []
});

db.createRole({
    role: "readOnlyAccess",
    privileges: [
        { resource: { db: "taskManagement", collection: "users" }, actions: ["find"] }
    ],
    roles: []
});

print("Initialization script executed successfully");

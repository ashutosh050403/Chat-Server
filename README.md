# Java Socket Chat Server

This is a simple chat server made using Java sockets.  
It lets many users connect, log in with a username, and talk to each other in real time.  
It does not use any web framework or database — only basic Java networking.

---

## How It Works

1. You start the server once.
2. People (clients) connect to the server from different terminals.
3. Each person must log in using a unique username.
4. After login, they can send public messages, see who’s online, send private messages, or leave the chat.
5. When someone disconnects, the server tells everyone else.

---

## Commands You Can Use

After connecting, type these commands:

| Command | Description |
|----------|--------------|
| `LOGIN <username>` | Log in. Must be the first thing you send after connecting. |
| `MSG <text>` | Send a message to everyone. |
| `WHO` | See a list of all people currently connected. |
| `DM <username> <text>` | Send a private message to one person. |
| `PING` | Check if the server is still alive. Server replies `PONG`. |
| `QUIT` | (Optional if added in code) Leave the chat. |

Example:
LOGIN Alice
OK
MSG Hello everyone!

## What You Need

- Java installed on your computer (JDK 11 or newer).

Check if Java is installed:
```bash
java -version

How to Run
1. Open a terminal in the folder where you saved ChatServer.java.
2. Compile the code:
javac ChatServer.java

3. Start the server:
java ChatServer


You should see:

ChatServer listening on port 4000


The server is now waiting for people to join.

How to Connect Clients

You can open more terminals to act as different users.

If you are on Windows

Using Ncat

If Telnet is not working or you want a simpler way:

Install Nmap
 which includes ncat.

Then open a terminal and type:

ncat localhost 4000


Once connected, type:

LOGIN Ashu

Example Chat

Client 1 (Ashu):

LOGIN Ashu
OK
MSG Hello everyone!


Client 2 (Dev):

LOGIN Dev
OK
MSG Hi Ashu!


Both clients will see:

MSG Ashu Hello everyone!
MSG Dev Hi Alice!


If Ashu closes her terminal or presses Ctrl + C, Dev will see:

INFO Ashu disconnected

Disconnecting

To leave the chat:

If using ncat or nc → press Ctrl + C

Common Problems and Fixes
1. ERR expected-login

You forgot to log in. Type:

LOGIN <yourname>


and press Enter right after connecting.

2. ERR username-taken

Someone else is already using that username. Try another name.

3. ERR login-timeout

You waited too long after connecting. Reconnect and type LOGIN <username> quickly.

4. ERR unknown-command

You typed something that the server doesn’t understand.
Use one of the supported commands (MSG, WHO, DM, PING, etc.).

5. Connection refused or Connection timed out

The server is not running. Start it with:

java ChatServer

6. 'nc' is not recognized

On Windows, use ncat.

[Chat Server Demo (Google Drive)](https://drive.google.com/file/d/1Plkuf6Yr5aRHEYaxPu5nOp3Yal8OC9L7/view)


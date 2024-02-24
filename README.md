# Sistemas-Multi-Agente
Developed by Tiago Varela, José Domingues, and Brian Marques for a University Course.

Java Multi-Agent System capable of playing the board game Secret Hitler. 

Uses Jade Framework for Agent communication.

##Execution

To execute the project 7 agents are necessary: 1 GameManager e 6 Players.
All agents must have different names for the correct execution of human players.

A GameManager agent may be created with:
NAME:agent.communication.GameManager

A PlayerAgent agent may be created with:
NAME:agent.communication.PlayerAgent(BEHAVIOUR_TYPE,VERBOSE)

Players may have various types (BEHAVIOUR_TYPE):
shy        - Agent with little decision capacity. Activates with "shy" as its first argument.
aggressive - Aggressive agent. Activates with "aggressive" as its first argument.
calm       - Activates human player. This player is controlled through user input in the console. Activates with "calm" as its first argument.
default    - Default agent behaviour. Activates with any string as its first argument, or with an empty first argument.

Players may be verbose (VERBOSE):
true    - The second argument as "true" activates verbose mode, which makes the agent print every decision they make. (Multiple verbose agents simultaneously may make the output difficult to read or interpret due to sheer volume.)
default - If there is no second argument or it isn't "true", verbose mode does not activate.

Example string to execute the program on Windows:
java -classpath PATH_TO_PROJECT_BIN_FOLDER;PATH_TO_JADE_JAR jade.Boot -gui gameManager:agent.communication.GameManager;player1:agent.communication.PlayerAgent(shy);player2:agent.communication.PlayerAgent(,true);player3:agent.communication.PlayerAgent(aggressive);player4:agent.communication.PlayerAgent();player5:agent.communication.PlayerAgent(calm);player6:agent.communication.PlayerAgent(shy)

(Shy: player1 e player6; Default: player2 e player4; Aggressive: player3; Human: player4; Verbose: player2)

WARNING:
The communication protocol utilises while(true) whilst waiting for the next message. This consumes more processing power than necessary, and is a flaw we would've liked to remove.
Playing with a human player increases wait time and therefore wastes more processing.
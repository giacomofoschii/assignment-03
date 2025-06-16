package pcd.ass03.controller

import pcd.ass03.distributed._

import scala.io.StdIn

object Main:
  def main(args: Array[String]): Unit =
    if args.isEmpty then
      println("Usage: DistributedMain <mode> [options]")
      println("Modes:")
      println("  server              - Start the game server")
      println("  client <name>       - Start a player client")
      println("  ai <count>          - Start AI players")
      println("  all                 - Start server + 3 AI + wait for client")
      System.exit(1)
      
    args(0).toLowerCase match
      case "server" =>
        GameServer.main(Array.empty)

      case "client" =>
        val name = if args.length > 1 then args(1) else {
          println("Please provide a player name")
          StdIn.readLine()
        }
        GameClient.main(Array(name))

      case "ai" =>
        val count = if args.length > 1 then args(1) else "3"
        AIClient.main(Array(count))

      case "all" =>
        val serverThread = new Thread(() => GameServer.main(Array.empty))
        serverThread.start()

        // Aspetta che il server si avvii
        Thread.sleep(3000)

        // Avvia AI
        val aiThread = new Thread(() => AIClient.main(Array("3")))
        aiThread.start()

        Thread.sleep(2000)

        // Avvia client interattivo
        println("Server and AI players started. Press ENTER to start a human player...")
        StdIn.readLine()
        GameClient.main(Array.empty)

      case _ =>
        println(s"Unknown mode: ${args(0)}")
        System.exit(1)
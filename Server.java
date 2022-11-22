import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class Server {

  public static HashMap<Integer, GameThread> games;

  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(7777);
      Socket waitingPlayer = null;
      games = new HashMap<>();
      while (true) {
        Socket newComer = serverSocket.accept();
        if (waitingPlayer == null) {
          waitingPlayer = newComer;
          System.out.print("一个玩家正在等待:");
          System.out.printf("%s\n", waitingPlayer.getRemoteSocketAddress());

          PrintWriter writer = new PrintWriter(waitingPlayer.getOutputStream());
          writer.println("正在匹配玩家，请等待...");
          writer.flush();
        } else {
          System.out.print("两个玩家配对成功:");
          System.out.printf("<%s><%s>\n", waitingPlayer.getRemoteSocketAddress(),
              newComer.getRemoteSocketAddress());
          int id = new Random().nextInt(100) + 100;
          new GameThread(id, serverSocket, waitingPlayer, newComer).start();

          waitingPlayer = null;
        }
      }
    } catch (IOException e) {
      System.out.println("游戏异常");
      e.printStackTrace();
    }

  }
}
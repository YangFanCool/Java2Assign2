import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class GameThread extends Thread {

  private final Object o = new Object();
  private final int id;
  private final ServerSocket serverSocket;
  private final Socket s1;
  private final Socket s2;
  private final char[][] board;
  private char winner;

  private char currentPlayer;


  public GameThread(int id, ServerSocket serverSocket, Socket s1, Socket s2) {
    this.id = id;
    this.serverSocket = serverSocket;
    this.s1 = s1;
    this.s2 = s2;
    this.winner = '*';
    this.board = new char[5][5];
    for (int i = 0; i <= 4; i++) {
      for (int j = 0; j <= 4; j++) {
        board[i][j] = '0';
      }
    }
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        board[i][j] = '0';
      }
    }
  }

  public char check() {
    char res = '*';
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        char cur = board[i][j];
        boolean b1 = (cur == board[i][j - 1]) && (cur == board[i][j + 1]);
        boolean b2 = (cur == board[i - 1][j]) && (cur == board[i + 1][j]);
        boolean b3 = (cur == board[i - 1][j + 1]) && (cur == board[i + 1][j - 1]);
        boolean b4 = (cur == board[i - 1][j - 1]) && (cur == board[i + 1][j + 1]);
        if (b1 || b2 || b3 || b4) {
          if (cur == '0') {
            res = '0';
          } else {
            return cur;
          }
        }
      }
    }
    return res;
  }

  public void send(String msg) {
    send(s1, msg);
    send(s2, msg);
  }

  public void send(Socket s, String msg) {
    PrintWriter to;
    try {
      to = new PrintWriter(s.getOutputStream());
      to.println("<GameID: !" + id + "!>" + msg);
      to.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public char set(int x, int y, char who) {
    switch (who) {
      case '1':
        board[x][y] = '1';
        break;
      case '2':
        board[x][y] = '2';
        break;
    }
    return check();
  }

  public String printBoard() {
    StringBuilder s = new StringBuilder("@");
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        s.append(board[i][j]);
      }
    }
    s.append("@");
    return s.toString();
  }

  public void over() throws IOException {
    s1.close();
    s2.close();

    String startMsg = String.format("Game Over ", id);

    System.out.println(startMsg);
  }

  public String receiveRequest(Socket socket) {
    String line = null;
    try {
      Scanner sc = new Scanner(socket.getInputStream());
      if (sc.hasNextLine()) {
        line = sc.nextLine();
      } else {
        send("玩家离线");
      }
    } catch (IOException e) {
      send("玩家离线");
    }
    System.out.println(line);
    return line;
  }

  @Override
  public void run() {
    try {

      String startMsg = String.format("Game Start ", id);

      System.out.println(startMsg);

      send(s1, startMsg + "<who:1>");
      send(s2, startMsg + "<who:2>");

      PlayerThread p1 = new PlayerThread('1', s1);
      PlayerThread p2 = new PlayerThread('2', s2);
      currentPlayer = '1';

      p1.start();
      p2.start();
      send("NOW" + printBoard());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class PlayerThread extends Thread {

    char who;
    Socket socket;

    public PlayerThread(char who, Socket s) {
      this.who = who;
      this.socket = s;
    }

    @Override
    public void run() {
      try {
        synchronized (o) {
          while (true) {

            if (currentPlayer == who) {

              send(socket, "<Info: Your turn, please input ...>");

              String xy = receiveRequest(socket);

              if (xy != null) {
                int x = Integer.parseInt(String.valueOf(xy.charAt(0)));
                int y = Integer.parseInt(String.valueOf(xy.charAt(1)));

                winner = set(x, y, this.who);
              } else {
                winner = '0';
              }

              if (winner == '0') {
                send("NOW" + printBoard());
                String broadcast = String.format(
                    "<Info: Player %s successfully set, game continue ...>", who);
                send(broadcast);
              } else if (winner == '*') {
                send("NOW" + printBoard());
                String broadcast = "<Over@null@>";
                send(broadcast);
              } else {
                send("NOW" + printBoard());
                String broadcast = String.format("<Over:@%s@>", winner);
                send(broadcast);
              }

              if (who == '1') {
                currentPlayer = '2';
              }
              if (who == '2') {
                currentPlayer = '1';
              }
              o.notify();
            } else {

              send(socket, "<Info: Your opponent‘s turn, please wait ...>");
              o.wait();
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
}

package application.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class GameThread extends Thread {

  private  final Object o = new Object();//这两个变量用来控制反转用户线程
  private  int cnt;
  private  boolean over;
  private  int id;
  private  ServerSocket serverSocket;
  private  Socket s1;// player1 socket
  private  Socket s2;// player2 socket
  private  char[][] board;
  private  char winner;// 0表示平局，1，2分别表示玩家1，2获胜

  public GameThread(int id, ServerSocket serverSocket, Socket s1, Socket s2) {
    this.id = id;
    this.serverSocket = serverSocket;
    this.s1 = s1;
    this.s2 = s2;
    this.cnt = 9;
    this.over = false;
    this.winner = 0;
    this.board = new char[5][5];
    for (int i = 0; i <= 4; i++) {
      for (int j = 0; j <= 4; j++) {
        board[i][j] = '*';
      }
    }
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        board[i][j] = '0';
      }
    }
    /*
     【·】【·】【·】【·】【·】
     【·】【1】【2】【3】【·】
     【·】【1】【2】【3】【·】
     【·】【1】【2】【3】【·】
     【·】【·】【·】【·】【·】
   */
  }

  public  char check() {
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        char cur = board[i][j];//cur 是当前正在判断的棋子
        boolean b1 = (cur == board[i][j - 1]) && (cur == board[i][j + 1]);// ——
        boolean b2 = (cur == board[i - 1][j]) && (cur == board[i + 1][j]);// ｜
        boolean b3 = (cur == board[i - 1][j + 1]) && (cur == board[i + 1][j - 1]);// /
        boolean b4 = (cur == board[i - 1][j - 1]) && (cur == board[i + 1][j + 1]);// \
        if (b1 || b2 || b3 || b4) {
          return cur;
        }
      }
    }
    return '0';
  }

  public  void send(String msg) {
    send(s1,msg);
    send(s2,msg);
  }

  public  void send(Socket s, String msg) {
    PrintWriter to;
    try {
      to = new PrintWriter(s.getOutputStream());
      to.println("<GameID: !"+id+"!>"+msg);
      to.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public  char set(int x, int y, int who) {
    switch (who) {
      case 1 : board[x][y] = '1';break;
      case 2 : board[x][y] = '2';break;
    }
    return check();
  }

  public  String printBoard(){
    StringBuilder s = new StringBuilder("@");
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        s.append(board[i][j]);
      }
    }
    s.append("@");
    return s.toString();
  }

  public  void  over() throws IOException {
    s1.close();
    s2.close();
    // 0 服务器端输出游戏结束信号
    String startMsg = String.format("Game Over ", id);
    // "游戏开始，房间ID为" + id;
    System.out.println(startMsg);
  }

  public  String receiveRequest(Socket socket) throws IOException {
    return new Scanner(socket.getInputStream()).nextLine();
  }

  @Override
  public void run() {
    try {
      // 0 服务器端输出游戏开始信号
      String startMsg = String.format("Game Start ", id);
      // "游戏开始，房间ID为" + id;
      System.out.println(startMsg);
      // 0 end

      // 1 先通知两位玩家房间号
      send(s1, startMsg + " You are player1");
      send(s2, startMsg + " You are player2");
      // 1 end

      // 2 新建两个监听用户请求，并做出相应回应的线程，让两个线程交替运行
      // 游戏最多9步
      PlayerController1 p1 = new PlayerController1(s1);
      PlayerController2 p2 = new PlayerController2(s2);
      p1.start();
      p2.start();
      // 2 end

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class PlayerController1 extends Thread {

    Socket s1;

    public PlayerController1(Socket s1) {
      this.s1 = s1;
    }

    @Override
    public void run() {
      try {
        synchronized (o) {
          while ((cnt > 0)&& !over) {
            if (cnt % 2 == 0) {
              o.wait();
            } else {
              // 1 给玩家发送指示信息
              send(s1, "<Input>"+printBoard()+"<xy>");
              // 2 <request> 玩家发来的坐标
              String xy = receiveRequest(s1);
              // 3 对棋盘进行操作，并判断胜负
              int x = Integer.parseInt(String.valueOf(xy.charAt(0)));
              int y = Integer.parseInt(String.valueOf(xy.charAt(1)));
              winner = set(x, y, 1);
              // 4 <response> 给玩家发送结果
              if (winner == '0') {
                String response = "<Succeed>"+printBoard()+"<waiting>";
                send(s1, response);
                cnt--;
                o.notify();
              } else {
                over = true;
                String response = String.format("<Game over>%s<winner player%s>",printBoard(),winner);
                send(response);
                cnt = 0;
              }
              // 5 server控制台输出此步骤的log信息
              // <GameID><Order><Player><x,y><winner>
              String log = String.format("<GameID: !%s!><Order: %s><Player: %s><x,y: %s><Winner: %s>",
                  id, (9 - cnt), "1", xy, winner);
              System.out.println(log);
              // 6 检查游戏结束状态
              if (over) over();
            }
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

   class PlayerController2 extends Thread {

    Socket s2;

    public PlayerController2(Socket s2) {
      this.s2 = s2;
    }

    @Override
    public void run() {
      try {
        synchronized (o) {
          while ((cnt > 0)&& !over) {
            if (cnt % 2 == 1) {
              o.wait();
            } else {
              // 1 给玩家发送指示信息
              send(s2, "<Input>"+printBoard()+"<xy>");
              // 2 <request> 玩家发来的坐标
              String xy = receiveRequest(s2);
              // 3 对棋盘进行操作，并判断胜负
              int x = Integer.parseInt(String.valueOf(xy.charAt(0)));
              int y = Integer.parseInt(String.valueOf(xy.charAt(1)));
              winner = set(x, y, 2);
              // 4 <response> 给玩家发送结果
              if (winner == '0') {
                String response = "<Succeed>"+printBoard()+"<waiting>";
                send(s2, response);
                cnt--;
                o.notify();
              } else {
                over = true;
                String response = String.format("<Game over><winner player%s>", winner);
                send(response);
                cnt = 0;
                over();
              }
              // 5 server控制台输出此步骤的log信息
              // <GameID><Order><Player><x,y><winner>
              String log = String.format("<GameID: !%s!><Order: %s><Player: %s><x,y: %s><Winner: %s>",
                  id, (9 - cnt), "2", xy, winner);
              System.out.println(log);
              // 6 检查游戏结束状态
              if (over) over();
            }
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}

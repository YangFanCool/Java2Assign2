package application.server;

import javafx.application.Platform;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class GameThread extends Thread {

  private  final Object o = new Object();//这两个变量用来控制反转用户线程
  private  int id;
  private  ServerSocket serverSocket;
  private  Socket s1;// player1 socket
  private  Socket s2;// player2 socket
  private  char[][] board;
  private  char winner;// 0表示平局，1，2分别表示玩家1，2获胜

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

  public  char check() {
    char res = '*';
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        char cur = board[i][j];//cur 是当前正在判断的棋子
        boolean b1 = (cur == board[i][j - 1]) && (cur == board[i][j + 1]);// ——
        boolean b2 = (cur == board[i - 1][j]) && (cur == board[i + 1][j]);// ｜
        boolean b3 = (cur == board[i - 1][j + 1]) && (cur == board[i + 1][j - 1]);// /
        boolean b4 = (cur == board[i - 1][j - 1]) && (cur == board[i + 1][j + 1]);// \
        if (b1 || b2 || b3 || b4 ) {
          if (cur=='0')res='0';
          else return cur;
        }
      }
    }
    return res;
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

  public  char set(int x, int y, char who) {
    switch (who) {
      case '1' : board[x][y] = '1';break;
      case '2' : board[x][y] = '2';break;
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
    return line;
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
      send(s1, startMsg + "<who:1>");
      send(s2, startMsg + "<who:2>");
      // 1 end

      // 2 新建两个监听用户请求，并做出相应回应的线程，让两个线程交替运行
      PlayerThread p1 = new PlayerThread('1',s1);
      PlayerThread p2 = new PlayerThread('2',s2);
      currentPlayer = '1';

      p1.start();
      p2.start();
      send("NOW"+printBoard());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class PlayerThread extends Thread{
    char who;
    Socket socket;

    public PlayerThread(char who,Socket s) {
      this.who = who;
      this.socket = s;
    }

    @Override
    public void run() {
      try{
        synchronized (o){
          while (true){
            //是当前玩家的回合
            if (currentPlayer == who){

              //向当前玩家发送提示信息，引导输入
              send(socket,"<Info: Your turn, please input ...>");
              //等待玩家的输入
              String xy = receiveRequest(socket);

              if (xy!=null){
                int x = Integer.parseInt(String.valueOf(xy.charAt(0)));
                int y = Integer.parseInt(String.valueOf(xy.charAt(1)));
                //set棋盘，判断胜负
                winner = set(x,y,this.who);
              }
              else winner='0';


              //向两个人广播刚才操作的结果(1没有胜负继续、2游戏结束平局、3游戏结束有一方获胜)
              if (winner=='0'){
                send("NOW"+printBoard());
                String broadcast = String.format("<Info: Player %s successfully set, game continue ...>",who);
                send(broadcast);
              }
              else if (winner=='*'){
                send("NOW"+printBoard());
                String broadcast = "<Over@null@>";
                send(broadcast);
              }
              else {
                send("NOW"+printBoard());
                String broadcast = String.format("<Over:@%s@>",winner);
                send(broadcast);
              }
              //最后释放锁，让对手的处理线程拿到CPU资源
              if (who =='1') currentPlayer ='2';
              if (who =='2') currentPlayer ='1';
              o.notify();
            }
            //不是当前玩家的回合
            else {
              //向这个线程对应的玩家发送提示信息，引导等待
              send(socket,"<Info: Your opponent‘s turn, please wait ...>");
              o.wait();
            }
          }
        }
      }
      catch (Exception e){
        e.printStackTrace();
      }
    }

  }
}

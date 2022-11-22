import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class PLayerController implements Initializable {

  private static final int BOUND = 90;
  private static final int OFFSET = 15;


  private static final boolean[][] flag = new boolean[3][3];
  private static char who = 1;
  private static String board = "000000000";
  @FXML
  private Text id_area;
  @FXML
  private Pane base_square;
  @FXML
  private Text info;

  private String info_area;
  @FXML
  private Text room_info;
  @FXML
  private Rectangle game_panel;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    Socket socket = null;
    Scanner response;
    PrintWriter request;
    try {
      socket = new Socket("127.0.0.1", 7777);
    } catch (Exception e) {
      System.out.println("连接服务器失败。。。");
      System.exit(0);
    }
    try {
      info.setText("hello");
      response = new Scanner(socket.getInputStream());
      request = new PrintWriter(socket.getOutputStream());

      Timer timer = new Timer();
      timer.schedule(new TimerTask() {
        public void run() {
          Platform.runLater(() -> {
            refresh(board);
          });
        }
      }, 100, 100);

      new Thread(() -> {
        while (response.hasNextLine()) {
          String line = response.nextLine();
          System.out.println(line);

          if (line.contains("NOW")) {
            board = line.split("@")[1];
          } else {
            if (line.contains("who")) {
              who = line.charAt(line.length() - 2);
              id_area.setText("Player id: " + who);

              room_info.setText("Game id: " + line.split("!")[1]);

              setText("please wait for opponent player");
            }
            if (line.contains("input")) {

              setText("its your turn, please choose");
            } else if (line.contains("continue")) {
              setText(line);
            } else if (line.contains("Over")) {
              String winner = line.split("@")[1];
              System.out.println(winner);
              if (Objects.equals(winner, "null")) {

                setText("Game Over, No one win the game!");
              } else {
                setText("Game Over, Player " + winner + " win the game!");
              }
              try {
                Thread.sleep(8000);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              System.exit(0);
            } else if (line.contains("正在匹配玩家，请等待...")) {
              setText("please wait for another play join the game");

            } else if (line.contains("玩家离线")) {
              setText("玩家离开...(3秒后自动关闭)");

              try {
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              System.exit(0);
            }
          }
        }

        setText("服务器离线...(3秒后自动关闭)");
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        System.exit(0);
      }).start();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    game_panel.setOnMouseClicked(event -> {
      if (info_area.equals("its your turn, please choose")) {
        int x = (int) (event.getX() / BOUND);
        int y = (int) (event.getY() / BOUND);

        String s = String.valueOf(y + 1) + (x + 1);
        request.println(s);
        request.flush();

          if (who == '1') {
              drawLine(x, y);
          } else {
              drawCircle(x, y);
          }
      } else {
      }
    });
  }


  public void setText(String msg) {
    try {
      info_area = msg;
      info.setText(info_area);
    } catch (NullPointerException e) {
      System.out.println("Exception");
    }
  }

  public void refresh(String board) {
    int cnt;
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        cnt = 3 * i + j;
        if (board.charAt(cnt) == '1') {
          drawLine(j, i);
        } else if (board.charAt(cnt) == '2') {
          drawCircle(j, i);
        }
      }
    }

  }

  private void drawCircle(int i, int j) {

    Circle circle = new Circle();
    base_square.getChildren().add(circle);
    circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
    circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
    circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
    circle.setStroke(Color.RED);
    circle.setFill(Color.TRANSPARENT);
    flag[i][j] = true;
  }

  private void drawLine(int i, int j) {
    Line line_a = new Line();
    Line line_b = new Line();
    base_square.getChildren().add(line_a);
    base_square.getChildren().add(line_b);
    line_a.setStartX(i * BOUND + OFFSET * 1.5);
    line_a.setStartY(j * BOUND + OFFSET * 1.5);
    line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
    line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
    line_a.setStroke(Color.BLUE);
    line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
    line_b.setStartY(j * BOUND + OFFSET * 1.5);
    line_b.setEndX(i * BOUND + OFFSET * 1.5);
    line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
    line_b.setStroke(Color.BLUE);
    flag[i][j] = true;
  }
}
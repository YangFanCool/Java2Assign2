package application.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerClientController implements Initializable {

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
        Socket socket;

        Scanner response;
        PrintWriter request;
        try {
            info.setText("hello");
            socket = new Socket("127.0.0.1", 7777);
            response = new Scanner(socket.getInputStream());
            request = new PrintWriter(socket.getOutputStream());

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    Platform.runLater(() -> {
                        refresh(board);  //更新ui代码
                    });
                }
            }, 100, 100);

            new Thread(() -> {
                while (response.hasNextLine()) {
                    String line = response.nextLine();
                    System.out.println(line);
                    if (line.contains("You are player")) {
                        who = line.charAt(line.length() - 1);
                        id_area.setText("Player id: " + who);
                        info_area="please wait for opponent player";
                        info.setText(info_area);
                        room_info.setText("Game id: " + line.split("!")[1]);
                    } else if (line.contains("Input")) {
                        info_area="its your turn, please choose";
                        info.setText(info_area);
                        //刷新棋盘状态
                        board = line.split("@")[1];
                    } else if (line.contains("Succeed")) {
                        info_area="succeed, please wait for opponent player";
                        info.setText(info_area);
                        //刷新棋盘状态
                        board = line.split("@")[1];
                    } else if (line.contains("Game over")) {
                        char winner = line.charAt(line.length() - 2);
                        board = line.split("@")[1];
                        info_area="Game over, winner player is player" + winner;
                        info.setText(info_area);
                    } else if (line.contains("正在匹配玩家，请等待...")) {
                        info_area="please wait for another play join the game";
                        info.setText(info_area);
                    }
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        game_panel.setOnMouseClicked(event -> {
            if (info_area.equals("its your turn, please choose")){
                int x = (int) (event.getX() / BOUND);
                int y = (int) (event.getY() / BOUND);
                System.out.print("用户点击坐标");
                System.out.println((y + 1) + "," + (x + 1));
                //向服务器发送这一步点击的棋子
                String s = String.valueOf(y + 1) + (x + 1);
                request.println(s);
                request.flush();
                //标记当前下的棋子
                if (who == '1') drawLine(x, y);
                else drawCircle(x, y);
            }
        });


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

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.zip.CheckedInputStream;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;


public class game2048 extends JPanel{
    enum gameStates {
        START,
        RUNNING,
        WIN,
        OVER
    }

    final private static Color[] gridColors = {
            new Color(0x701710), new Color(0xFFE4C3), new Color(0xfff4d3),
            new Color(0xffdac3), new Color(0xe7b08e), new Color(0xe7bf8e),
            new Color(0xffc4c3), new Color(0xE7948e), new Color(0xbe7e56),
            new Color(0xbe5e56), new Color(0x9c3931), new Color(0x701710)
    };

    final private Color tableColor = new Color(0xBBADA0);

    final private Color nullColor = new Color(0xCDC1B4);

    final private Color startColor = new Color(0xFFEBCD);

    final private static int target = 2048;

    final private int size = 4;

    final private int panelX = 200, panelY = 100, panelW = 600, panelH = 600, panelR = 20;

    final private int tablePanelX = 220, tablePanelY = 120, tablePanelW = 560, tablePanelH = 560, tablePanelR = 18;

    final private int gridLength = 125;

    final private int largeString = 150, smallString = 30;

    private static int highScoreNum = 5;

    private static int[] highScores = new int[highScoreNum];

    private static int highestNum = -1;

    private static int score = 0;

    private static Grid[][] table;

    private gameStates curState = gameStates.START;

    private boolean moveCheckLock = false;
    
    private String leaderBoardPath = "LeaderBoard.txt";

    private Random rand = new Random();

    private FileDialog saveDialog, loadDialog;

    private static JFrame f;

    private void tableInit(Graphics2D g) {
        g.setColor(startColor);
        g.fillRoundRect(tablePanelX, tablePanelY, tablePanelW, tablePanelH, tablePanelR, tablePanelR);
    }

    private void boardInit(Graphics2D g) {
        g.setColor(startColor.darker());
        g.setFont(new Font("楷体", Font.BOLD, smallString * 2 / 3));
        g.drawString("最高分：", tablePanelX + tablePanelW + 40, tablePanelY - 60);
        for (int i = 0; i < 5; i++) {
            int rank = i + 1;
            g.drawString("第" + rank + "名： " + highScores[i], tablePanelX + tablePanelW + 55,
                    tablePanelY - 60 + 30 * (i + 1));
        }
    }

    private void menuInit(Graphics2D g) {
        g.setColor(startColor.darker());
        g.setFont(new Font("楷体", Font.BOLD, largeString));
        g.drawString("2048", 305, 350);
        g.setFont(new Font("楷体", Font.BOLD, smallString));
        g.drawString("开始游戏", 440, 500);
        g.setFont(new Font("楷体", Font.BOLD, smallString));
        g.drawString("提示：使用上下左右箭头移动图块", 280, 600);
    }

    // remain to be debugged
    private void gameEnd(Graphics2D g) {
        g.setColor(startColor.darker());
        g.setFont(new Font("楷体", Font.BOLD, largeString));
        g.drawString("2048", 305, 350);
        Font font = new Font("楷体", Font.BOLD, smallString);
        g.setFont(font);
        String scoreString = "本次你的得分为" + String.valueOf(score);
        Rectangle2D rect = font.getStringBounds(scoreString, g.getFontRenderContext());
        double stringW = rect.getWidth();
        g.drawString(scoreString, panelX + panelW / 2 - (int) (stringW / 2), panelY + 2 * panelH / 3 + 50);
        if (curState == gameStates.WIN) {
            g.drawString("恭喜你，你成功得到2048！再来一局？", 250, 500);
        } else if (curState == gameStates.OVER) {
            g.drawString("你失败了,请重新开始游戏", 330, 500);
        }
    }

    private void drawTable(Graphics2D g) {
        g.setColor(startColor.darker());
        Font font = new Font("楷体", Font.BOLD, smallString);
        g.setFont(font);
        String scoreString = "score: " + String.valueOf(score);
        Rectangle2D rect = font.getStringBounds(scoreString, g.getFontRenderContext());
        double stringW = rect.getWidth();
        g.drawString(scoreString, panelX + panelW / 2 - (int) (stringW / 2), panelY - 10);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (table[r][c] == null) {
                    g.setColor(nullColor);
                    g.fillRoundRect(tablePanelX + (gridLength + tablePanelX - panelX) * c,
                            tablePanelY + (gridLength + tablePanelY - panelY) * r, gridLength, gridLength, 9, 9);
                } else {
                    drawGrid(g, r, c);
                }
            }
        }
    }

    private void drawGrid(Graphics2D g, int r, int c) {
        int value = table[r][c].getValue();
        g.setColor(gridColors[(int) (Math.log(value) / Math.log(2)) + 1]);
        g.fillRoundRect(tablePanelX + (gridLength + tablePanelX - panelX) * c,
                tablePanelY + (gridLength + tablePanelY - panelY) * r, gridLength, gridLength, 9, 9);
        String valString = String.valueOf(value);
        if (value < 128) {
            g.setColor(gridColors[0]);
        } else {
            g.setColor(gridColors[1]);
        }
        Font font = new Font("楷体", Font.BOLD, 48);
        g.setFont(font);
        Rectangle2D rect = font.getStringBounds(valString, g.getFontRenderContext());
        double stringW = rect.getWidth();
        FontMetrics fm = g.getFontMetrics();
        int stringH = fm.getHeight();
        int ascent = fm.getAscent();
        int x = tablePanelX + (gridLength + tablePanelX - panelX) * c + (int) ((gridLength - stringW) / 2);
        int y = tablePanelY + (gridLength + tablePanelY - panelY) * r + (ascent + (gridLength - stringH) / 2);
        g.drawString(valString, x, y);
    }

    private void drawPanel(Graphics2D g) {
        boardInit(g);
        g.setColor(tableColor);
        g.fillRoundRect(panelX, panelY, panelW, panelH, panelR, panelR);
        if (curState == gameStates.RUNNING) {
            drawTable(g);
            return;
        } else if (curState == gameStates.START) {
            tableInit(g);
            menuInit(g);
            return;
        } else {
            tableInit(g);
            gameEnd(g);
            return;
        }
    }

    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPanel(g);
    }

    private String getPath()
    {
    	String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    	if(System.getProperty("os.name").contains("dows"))
    	{
    		path = path.substring(1,path.length());
    	}
    	if(path.contains("jar"))
    	{
    		path = path.substring(0,path.lastIndexOf("."));
    		return path.substring(0,path.lastIndexOf("/"));
    	}
    	return path.replace("target/classes/", "");
    }

    private void randomGridGenerator() {
        int pos = rand.nextInt(size * size);
        int r, c;
        do {
            pos = (pos + 1) % (size * size);
            r = pos / size;
            c = pos % size;
        } while (table[r][c] != null);
        table[r][c] = new Grid((rand.nextInt(1926) % 2 == 0 ? 2 : 4));
    }

    private void clearMerged() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (table[r][c] != null) {
                    table[r][c].setMerged();
                }
            }
        }
    }

    private void updateScores() {
        for (int i = 0; i < highScoreNum; i++) {
            if (score > highScores[i]) {
                for (int j = highScoreNum - 1; j > i; j--) {
                    highScores[j] = highScores[j - 1];
                }
                highScores[i] = score;
                try {
                    File leaderBoard = new File(leaderBoardPath);
                    if (!leaderBoard.isFile() || !leaderBoard.exists()) {

                        leaderBoard.createNewFile();
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(leaderBoardPath));
                    for (int k = 0; k < highScoreNum; k++) {
                        bw.write(highScores[k] + "\n");
                    }
                    bw.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "最高分保存失败！", "Warning", JOptionPane.WARNING_MESSAGE);
                    requestFocus();
                }
                break;
            }
        }
    }

    private boolean moveAvailable() {
        moveCheckLock = true;
        boolean canMove = moveUp() || moveDown() || moveLeft() || moveRight();
        moveCheckLock = false;
        return canMove;
    }

    private boolean move(int countDownFrom, int yIncr, int xIncr) {
        boolean moved = true;
        boolean movedOnce = false;
        while (moved)
        {
            moved = false;
            for (int i = 0; i < size * size; i++) {
                int j = Math.abs(countDownFrom - i);
                int r = j / size;
                int c = j % size;
                if (table[r][c] == null) {
                    continue;
                }
                int nextR = r + yIncr;
                int nextC = c + xIncr;
                while (nextR >= 0 && nextR < size && nextC >= 0 && nextC < size) {
                    Grid next = table[nextR][nextC];
                    Grid cur = table[r][c];
                    if (next == null) {
                        if (moveCheckLock) {
                            return true;
                        }
                        table[nextR][nextC] = cur;
                        table[r][c] = null;
                        r = nextR;
                        c = nextC;
                        nextR += yIncr;
                        nextC += xIncr;
                        moved = true;
                        movedOnce = true;
                    } else if (next.mergeable(cur)) {
                        if (moveCheckLock) {
                            return true;
                        }
                        int value = next.mergeWith(cur);
                        highestNum = (value > highestNum ? value : highestNum);
                        score += (value / 2);
                        table[r][c] = null;
                        moved = true;
                        movedOnce = true;
                        break;
                    } else {
                        break;
                    }
                }
            }
        }
        if (movedOnce) {
            if (highestNum < target) {
                clearMerged();
                randomGridGenerator();
                if (!moveAvailable()) {
                    curState = gameStates.OVER;
                    updateScores();
                }
            } else if (highestNum == target) {
                curState = gameStates.WIN;
                updateScores();
            }
        }

        
        return moved;
    }

    private boolean moveUp() {
        return move(0, -1, 0);
    }

    private boolean moveLeft() {
        return move(0, 0, -1);
    }

    private boolean moveDown() {
        return move(size * size - 1, 1, 0);
    }

    private boolean moveRight() {
        return move(size * size - 1, 0, 1);
    }
    
    private void startGame() {
        if (curState == gameStates.RUNNING) {
            return;
        }
        score = 0;
        highestNum = 0;
        curState = gameStates.RUNNING;
        table = new Grid[size][size];
        randomGridGenerator();
        randomGridGenerator();
    }

    public game2048() {
        JPanel loadPanel = new JPanel();
        JButton saveButton = new JButton("保存");
        JButton loadButton = new JButton("加载");

        try {
            leaderBoardPath = getPath() + "/" + leaderBoardPath;
            System.out.println(leaderBoardPath);
            File leaderBoard = new File(leaderBoardPath);
            if (leaderBoard.isFile() && leaderBoard.exists()) {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(leaderBoard), "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                String lineTxt = null;
                int i = 0;
                while ((lineTxt = br.readLine()) != null && i < highScoreNum) {
                    highScores[i] = Integer.valueOf(lineTxt);
                    i++;
                }
                br.close();
            } else {
                leaderBoard.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(leaderBoardPath));
                for (int i = 0; i < highScoreNum; i++) {
                    highScores[i] = 0;
                    bw.write("0\n");
                }
                bw.close();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "最高分读入失败！", "Warning", JOptionPane.WARNING_MESSAGE);
            requestFocus();
        }

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (curState != gameStates.RUNNING) {
                    JOptionPane.showMessageDialog(null, "请先开始一局游戏！", "Warning", JOptionPane.WARNING_MESSAGE);
                } else {
                    saveDialog = new FileDialog(f, "保存", FileDialog.SAVE);
                    saveDialog.setVisible(true);
                    String absPath = saveDialog.getDirectory() + saveDialog.getFile();

                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(absPath));
                        for (int i = 0; i < size * size; i++) {
                            if (table[i / size][i % size] == null) {
                                writer.write(-1 + "\n");
                            } else {
                                writer.write(table[i / size][i % size].getValue() + "\n");
                            }
                        }
                        writer.write(score + "\n");
                        writer.close();
                    } catch (IOException exc) {
                        exc.getMessage();
                    }
                }

                repaint();

                requestFocus();
            }
        });
        saveButton.setBounds(200, 20, 30, 20);
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (curState != gameStates.RUNNING) {
                    JOptionPane.showMessageDialog(null, "请先开始一局游戏！", "Warning", JOptionPane.WARNING_MESSAGE);
                } else {
                    loadDialog = new FileDialog(f, "加载", FileDialog.LOAD);
                    loadDialog.setVisible(true);
                    String absPath = loadDialog.getDirectory() + loadDialog.getFile();

                    if (loadDialog.getFile() == null) {
                        System.out.println(loadDialog.getFile());
                        requestFocus();
                        return;
                    }

                    BufferedReader reader = null;
                    Boolean winFlag = false, badFileFlag = false;
                    int[] tempCheck = new int[17];
                    try{
                        reader = new BufferedReader(new FileReader(absPath));
                        String str = null;
                        int i = 0;
                        while ((str = reader.readLine()) != null && i < size * size){
                            int checkNum = Integer.valueOf(str);
                            double check2 = Math.log(checkNum) / Math.log(2);
                            if(Math.abs(check2 - Math.round(check2)) < Double.MIN_VALUE && checkNum >= 2 && checkNum <= 2048){
                                tempCheck[i] = checkNum;
                                if(checkNum == 2048){
                                    winFlag = true;
                                }
                            }
                            else if(checkNum == -1){
                                tempCheck[i] = checkNum;
                            }
                            else {
                                badFileFlag = true;
                                winFlag = false;
                                break;
                            }
                            i++;
                        }
                        if (!badFileFlag && i == 16) {
                            if (Integer.valueOf(str) % 2 == 0) {
                                score = Integer.valueOf(str);
                                for (int j = 0; j < size * size && !winFlag; j++) {
                                    if (tempCheck[j] == -1) {
                                        table[j / size][j % size] = null;
                                    } else {
                                        table[j / size][j % size] = new Grid(tempCheck[j]);
                                    }
                                }
                            } else {
                                winFlag = false;
                                JOptionPane.showMessageDialog(null, "游戏记录不合法！", "Warning", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                        else {
                            winFlag = false;
                            JOptionPane.showMessageDialog(null, "游戏记录不合法！", "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                        reader.close();
                    } catch (IOException exc) {
                        exc.getMessage();
                    }
                        
                    if (winFlag) {
                        curState = gameStates.WIN;
                        updateScores();
                    } else if (!moveAvailable()) {
                        curState = gameStates.OVER;
                        updateScores();
                    } else {
                        curState = gameStates.RUNNING;
                    }
                }

                repaint();

                requestFocus();
            }
        });
        loadButton.setBounds(250, 20, 30, 20);
        loadPanel.add(saveButton);
        loadPanel.add(loadButton);
        loadPanel.setBackground(new Color(0xFAF8EF));
        this.add(loadPanel);

        setPreferredSize(new Dimension(1025, 800));
        setBackground(new Color(0xFAF8EF));
        setFont(new Font("楷体", Font.BOLD, 48));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (curState != gameStates.RUNNING) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        moveUp();
                        break;
                    case KeyEvent.VK_DOWN:
                        moveDown();
                        break;
                    case KeyEvent.VK_LEFT:
                        moveLeft();
                        break;
                    case KeyEvent.VK_RIGHT:
                        moveRight();
                        break;
                }
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startGame();
                repaint();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setTitle("2048小游戏");
            f.setResizable(false);
            f.add(new game2048(), BorderLayout.CENTER);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

class Grid {
    private boolean merged = false;
    private int value = -1;

    Grid(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public void setMerged() {
        this.merged = false;
    }

    boolean mergeable(Grid next) {
        return (!merged) && (next != null) && (!next.merged) && ((this.value ^ next.getValue()) == 0);
    }

    public int mergeWith(Grid next) {
        if (mergeable(next)) {
            this.value <<= 1;
            return this.value;
        } else {
            return -1;
        }
    }
}

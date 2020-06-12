package pkg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

// p129 proposes a text based game of minecraft. first thought goes to
// interface. I could make it scrolly. Or, I could try to get the slashie
// library working. Wouldn't hurt to have a quick shot at that.
//
// Slashie library doesn't work. Going to plain text. OK. Got a cls working.

class Terminal {
    private static Scanner scanner;

    private static void cls() {
        System.out.print("\033[H\033[2J");
    }

    private static String getLine() {
        String out; {
            try {
                out = scanner.next();
            }
            catch (NoSuchElementException e) {
                out = "";
            }
        }
        return out;
    }

    private static String prompt(String prompt) {
        System.out.printf("%s> ", prompt);
        String s = Terminal.getLine();
        return s;
    }

    public static void printStringArr(String[] arr) {
        for (String s: arr) {
            System.out.println(s);
        }
        System.out.flush();
    }

    public static void pauseForOk() {
        System.out.printf("say ok> ");
        Terminal.getLine();
    }

    public static void open() {
        Terminal.scanner = new Scanner(System.in);
        Terminal.cls();
    }
    public static void close() {
        Terminal.scanner.close();
    }

    public static String gameInteraction(String[] lines) {
        Terminal.cls();
        Terminal.printStringArr(lines);

        String s = prompt("command");
        return s;
    }

    public static String menuInteraction(String[] options) {
        Terminal.cls();
        Terminal.printStringArr(options);

        String s = prompt("menu");
        return s;
    }
}


class Coord {
    int s;
    int e;

    Coord() { }
    Coord(int s, int e) {
        this.s = s;
        this.e = e;
    }

    public boolean equals(Object obj) {
        Coord other = (Coord) obj;
        if (other.s != this.s) {
            return false;
        }
        if (other.e != this.e) {
            return false;
        }
        return true;
    }
}


class Game {
    // SQ square type
    private static final byte SQ_BOMB_LIVE    = (byte) 0xff;      // An unmarked bomb square
    private static final byte SQ_BOMB_EXPLODED= (byte) 0xfe;      // An exploded bomb
    private static final byte SQ_BOMB_MARKED  = (byte) 0xfd;      // A correctly marked bomb
    private static final byte SQ_QMARK        = (byte) 0xfc;      // Final screen shows ? for bad marks
    private static final byte SQ_BOMB_DEFUSED = (byte) 0xfb;      // Final screen shows d for bad marks
    private static final byte SQ_BAD_8        = (byte) 0x38;      // BAD: Safe spaces, incorrectly marked
    private static final byte SQ_BAD_7        = (byte) 0x37;      // .
    private static final byte SQ_BAD_6        = (byte) 0x36;      // .
    private static final byte SQ_BAD_5        = (byte) 0x35;      // .
    private static final byte SQ_BAD_4        = (byte) 0x34;      // .
    private static final byte SQ_BAD_3        = (byte) 0x33;      // .
    private static final byte SQ_BAD_2        = (byte) 0x32;      // .
    private static final byte SQ_BAD_1        = (byte) 0x31;      // .
    private static final byte SQ_BAD_0        = (byte) 0x30;      // .
    private static final byte SQ_REV_8        = (byte) 0x28;      // REV: Revealed space
    private static final byte SQ_REV_7        = (byte) 0x27;      // .
    private static final byte SQ_REV_6        = (byte) 0x26;      // .
    private static final byte SQ_REV_5        = (byte) 0x25;      // .
    private static final byte SQ_REV_4        = (byte) 0x24;      // .
    private static final byte SQ_REV_3        = (byte) 0x23;      // .
    private static final byte SQ_REV_2        = (byte) 0x22;      // .
    private static final byte SQ_REV_1        = (byte) 0x21;      // .
    private static final byte SQ_REV_0        = (byte) 0x20;      // .
    private static final byte SQ_HID_8        = (byte) 0x18;      // HID: Unrevealed, unmarked non-bomb spots
    private static final byte SQ_HID_7        = (byte) 0x17;      // .
    private static final byte SQ_HID_6        = (byte) 0x16;      // .
    private static final byte SQ_HID_5        = (byte) 0x15;      // .
    private static final byte SQ_HID_4        = (byte) 0x14;      // .
    private static final byte SQ_HID_3        = (byte) 0x13;      // .
    private static final byte SQ_HID_2        = (byte) 0x12;      // .
    private static final byte SQ_HID_1        = (byte) 0x11;      // .
    private static final byte SQ_HID_0        = (byte) 0x10;      // .
    private static final byte SQ_VANILLA      = (byte) 0x00;      // Used for non-bomb spaces during game init.

    // GS game state
    private static final byte GS_RUNNING      = (byte) 0x00;
    private static final byte GS_DONE         = (byte) 0x01;

    private static Random random = new Random();

    private int height;
    private int width;
    private byte gameState;

    Game() {
        this.height = 9;
        this.width = 9;
        this.gameState = GS_RUNNING;
    }

    void go() throws Exception {
        int bombCount = 0;
        int marksUsed = 0;

        byte[][] grid = new byte[height][width]; {
            // Place the bombs
            for (int s=0; s<height; s++) {
                for (int e=0; e<width; e++) {
                    if (s==height/2+1 && e==width/2+1) {
                        // Do nothing. Middle square should always be safe
                        grid[s][e] = SQ_VANILLA;
                        continue;
                    }
                    int r = random.nextInt(25);
                    if (r < 2) {
                        grid[s][e] = SQ_BOMB_LIVE;
                        bombCount++;
                    }
                    else {
                        grid[s][e] = SQ_VANILLA;
                    }
                }
            }
            // Mark the non-bomb areas.
            for (int s=0; s<height; s++) {
                for (int e=0; e<width; e++) {
                    if (grid[s][e] == SQ_BOMB_LIVE) continue;

                    byte gridByte = SQ_HID_0;
                    if (s>0 && e>0) {
                        if (grid[s-1][e-1] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (s>0) {
                        if (grid[s-1][e] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (s>0 && e<width-1) {
                        if (grid[s-1][e+1] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (e>0) {
                        if (grid[s][e-1] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (e<width-1) {
                        if (grid[s][e+1] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (s<height-1 && e>0) {
                        if (grid[s+1][e-1] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (s<height-1) {
                        if (grid[s+1][e] == SQ_BOMB_LIVE) gridByte++;
                    }
                    if (s<height-1 && e<width-1) {
                        if (grid[s+1][e+1] == SQ_BOMB_LIVE) gridByte++;
                    }
                    grid[s][e] = gridByte;
                }
            }
        }

        String message = "";

        while (1==1) {
            if (this.gameState == GS_DONE) {
                String[] lines = new String[height+2];

                // Convert all non-visible states into visible states.
                for (int s=0; s<height; s++) {
                    StringBuilder sb = new StringBuilder();
                    for (int e=0; e<width; e++) {
                        byte gridByte = grid[s][e];
                        if (gridByte == SQ_BOMB_LIVE) {
                            grid[s][e] = SQ_BOMB_EXPLODED;
                        }
                        else if (gridByte == SQ_BOMB_MARKED) {
                            grid[s][e] = SQ_BOMB_DEFUSED;
                        }
                        else if (SQ_BAD_0 <= gridByte && gridByte <= SQ_BAD_8) {
                            grid[s][e] = SQ_QMARK;
                        }
                        else if (SQ_HID_0 <= gridByte && gridByte <= SQ_HID_8) {
                            if (gridByte == SQ_HID_0) {
                                grid[s][e] = SQ_REV_0;
                            }
                            else if (gridByte == SQ_HID_1) {
                                grid[s][e] = SQ_REV_1;
                            }
                            else if (gridByte == SQ_HID_2) {
                                grid[s][e] = SQ_REV_2;
                            }
                            else if (gridByte == SQ_HID_3) {
                                grid[s][e] = SQ_REV_3;
                            }
                            else if (gridByte == SQ_HID_4) {
                                grid[s][e] = SQ_REV_4;
                            }
                            else if (gridByte == SQ_HID_5) {
                                grid[s][e] = SQ_REV_5;
                            }
                            else if (gridByte == SQ_HID_6) {
                                grid[s][e] = SQ_REV_6;
                            }
                            else if (gridByte == SQ_HID_7) {
                                grid[s][e] = SQ_REV_7;
                            }
                            else if (gridByte == SQ_HID_8) {
                                grid[s][e] = SQ_REV_8;
                            }
                        }
                    }
                    sb.append(" ").append(s);
                    lines[s] = sb.toString();
                }
            }

            String[] lines = new String[height+6];
            for (int s=0; s<height; s++) {
                StringBuilder sb = new StringBuilder();
                for (int e=0; e<width; e++) {
                    byte gridByte = grid[s][e];
                    if (gridByte == SQ_BOMB_LIVE) {
                        sb.append("#");
                    }
                    else if (gridByte == SQ_BOMB_EXPLODED) {
                        sb.append("!");
                    }
                    else if (gridByte == SQ_BOMB_MARKED) {
                        sb.append("-");
                    }
                    else if (gridByte == SQ_QMARK) {
                        sb.append("?");
                    }
                    else if (gridByte == SQ_BOMB_DEFUSED) {
                        sb.append("d");
                    }
                    else if (SQ_BAD_0 <= gridByte && gridByte <= SQ_BAD_8) {
                        sb.append("-");
                    }
                    else if (SQ_REV_0 <= gridByte && gridByte <= SQ_REV_8) {
                        if (gridByte == SQ_REV_0) {
                            sb.append(" ");
                        }
                        else if (gridByte == SQ_REV_1) {
                            sb.append("1");
                        }
                        else if (gridByte == SQ_REV_2) {
                            sb.append("2");
                        }
                        else if (gridByte == SQ_REV_3) {
                            sb.append("3");
                        }
                        else if (gridByte == SQ_REV_4) {
                            sb.append("4");
                        }
                        else if (gridByte == SQ_REV_5) {
                            sb.append("5");
                        }
                        else if (gridByte == SQ_REV_6) {
                            sb.append("6");
                        }
                        else if (gridByte == SQ_REV_7) {
                            sb.append("7");
                        }
                        else if (gridByte == SQ_REV_8) {
                            sb.append("8");
                        }
                    }
                    else if (SQ_HID_0 <= gridByte && gridByte <= SQ_HID_8) {
                        sb.append("#");
                    }
                    else {
                        throw new Exception("Unhandled grid byte, "+gridByte);
                    }
                }
                sb.append(".").append(s);
                lines[s] = sb.toString();
            }

            lines[height] = ".........";
            lines[height+1] = "abcdefghi";
            lines[height+2] = "";
            lines[height+3] = new String(message); message = "";
            lines[height+4] = "";

            String cmd = Terminal.gameInteraction(lines);
            if (gameState == GS_DONE) {
                break;
            }
            else if (cmd.equals("?")) {
                message = "r: reveal; m: mark; c: count. Goal: Mark or reveal all spots.";
            }
            else if (cmd.equals("c")) {
                message = "Used "+marksUsed+"/"+bombCount+" marked for defuse.";
            }
            else if (cmd.startsWith("m")) {
                try {
                    if (cmd.length() != 3) throw new Exception("Wrong length.");
                    int s = Integer.parseInt(Character.valueOf(cmd.charAt(2)).toString());
                    int e = cmd.charAt(1) - 'a';
                    if (s >= height) throw new Exception("South value out-of-bounds.");
                    if (e >= width) throw new Exception("East value out-of-bounds.");

                    if (marksUsed >= bombCount) {
                        // It might be more fun if things just blow up if you
                        // have used up all the marks, with some in error.
                        message = "No marks left. You have an error somewhere.";
                    }
                    else if (grid[s][e] == SQ_BOMB_LIVE) {
                        grid[s][e] = SQ_BOMB_MARKED;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_BOMB_MARKED) {
                        grid[s][e] = SQ_BOMB_LIVE;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_0) {
                        grid[s][e] = SQ_HID_0;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_7) {
                        grid[s][e] = SQ_HID_7;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_6) {
                        grid[s][e] = SQ_HID_6;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_5) {
                        grid[s][e] = SQ_HID_5;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_4) {
                        grid[s][e] = SQ_HID_4;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_3) {
                        grid[s][e] = SQ_HID_3;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_2) {
                        grid[s][e] = SQ_HID_2;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_1) {
                        grid[s][e] = SQ_HID_1;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_BAD_0) {
                        grid[s][e] = SQ_HID_0;
                        message = "Unmarked "+(char) ('a'+e)+s+".";
                        marksUsed--;
                    }
                    else if (grid[s][e] == SQ_HID_7) {
                        grid[s][e] = SQ_BAD_7;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_6) {
                        grid[s][e] = SQ_BAD_6;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_5) {
                        grid[s][e] = SQ_BAD_5;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_4) {
                        grid[s][e] = SQ_BAD_4;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_3) {
                        grid[s][e] = SQ_BAD_3;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_2) {
                        grid[s][e] = SQ_BAD_2;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_1) {
                        grid[s][e] = SQ_BAD_1;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else if (grid[s][e] == SQ_HID_0) {
                        grid[s][e] = SQ_BAD_0;
                        message = "Marked "+(char) ('a'+e)+s+" for defusal.";
                        marksUsed++;
                    }
                    else {
                        message = "Spot is revealed, and cannot be marked.";
                    }
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                    Terminal.pauseForOk();

                    message = "Usage: 'm[es]' (e: East; s: South; no space in cmd)";
                }
            }
            else if (cmd.startsWith("r")) {
                try {
                    if (cmd.length() != 3) throw new Exception("Wrong length.");
                    int s = Integer.parseInt(Character.valueOf(cmd.charAt(2)).toString());
                    int e = cmd.charAt(1) - 'a';
                    if (s >= height) throw new Exception("South value out-of-bounds.");
                    if (e >= width) throw new Exception("East value out-of-bounds.");

                    byte sq = grid[s][e];
                    if (sq == SQ_BOMB_LIVE) {
                        this.gameState = GS_DONE;
                    }
                    else if (sq == SQ_BOMB_MARKED || sq == SQ_BAD_8 || sq == SQ_BAD_7 || sq == SQ_BAD_6 || sq == SQ_BAD_5 || sq == SQ_BAD_4 || sq == SQ_BAD_3 || sq == SQ_BAD_2 || sq == SQ_BAD_1) {
                        message = "No action taken, square was marked.";
                    }
                    else if (sq == SQ_REV_8 || sq == SQ_REV_7 || sq == SQ_REV_6 || sq == SQ_REV_5 || sq == SQ_REV_4 || sq == SQ_REV_3 || sq == SQ_REV_2 || sq == SQ_REV_1 || sq == SQ_REV_0) {
                        message = "No action taken, square already revealed.";
                    }
                    else if (sq == SQ_HID_8 || sq == SQ_HID_7 || sq == SQ_HID_6 || sq == SQ_HID_5 || sq == SQ_HID_4 || sq == SQ_HID_3 || sq == SQ_HID_2 || sq == SQ_HID_1 || sq == SQ_HID_0) {
                        revealHiddenSpace(grid, s, e);
                    }
                    else {
                        message = "Software error. Unhandled scenario.";
                    }
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                    Terminal.pauseForOk();

                    message = "Usage: 'm[es]' (e: East; s: South; no space in cmd)";
                }
            }
            else {
                message = "Unrecognised cmd ["+cmd+"] (try ?)";
            }

            if (marksUsed == bombCount) {
                this.gameState = GS_DONE;
            }
        }
    }

    void revealHiddenSpace(byte[][] grid, int s, int e) {
        Stack<Coord> q = new Stack();
        List<Coord> lst_seen = new ArrayList();
        Coord wCoord;

        wCoord = new Coord(s, e);
        q.push(wCoord);
        while (!q.empty()) {
            Coord coord = q.pop();
            lst_seen.add(coord);

            s = coord.s;
            e = coord.e;

            byte sq = grid[s][e];

            // We are only looking at SQ_HID_* in these if statements. The
            // other square types should already have been filtered before
            // the call to this.
            if (sq == SQ_HID_0) {
                // We look at surrounding squares if the current square is not
                // near a bomb.
                if (s > 0 && e > 0) {
                    wCoord = new Coord(s-1, e-1);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (s > 0) {
                    wCoord = new Coord(s-1, e);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (s > 0 && e<this.width-1) {
                    wCoord = new Coord(s-1, e+1);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (e > 0) {
                    wCoord = new Coord(s, e-1);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (e < this.width-1) {
                    wCoord = new Coord(s, e+1);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (s < this.height-1 && e > 0) {
                    wCoord = new Coord(s+1, e-1);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (s < this.height-1) {
                    wCoord = new Coord(s+1, e);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }
                if (s < this.height-1 && e < this.width-1) {
                    wCoord = new Coord(s+1, e+1);
                    if (!lst_seen.contains(wCoord)) q.push(wCoord);
                }

                grid[s][e] = SQ_REV_0;
            }
            else if (sq == SQ_HID_1) {
                grid[s][e] = SQ_REV_1;
            }
            else if (sq == SQ_HID_2) {
                grid[s][e] = SQ_REV_2;
            }
            else if (sq == SQ_HID_3) {
                grid[s][e] = SQ_REV_3;
            }
            else if (sq == SQ_HID_4) {
                grid[s][e] = SQ_REV_4;
            }
            else if (sq == SQ_HID_5) {
                grid[s][e] = SQ_REV_5;
            }
            else if (sq == SQ_HID_6) {
                grid[s][e] = SQ_REV_6;
            }
            else if (sq == SQ_HID_7) {
                grid[s][e] = SQ_REV_7;
            }
            else if (sq == SQ_HID_8) {
                grid[s][e] = SQ_REV_8;
            }
        }
    }

    public void revealAll(byte[][] grid) {
        
    }
}


class Engine {
    Engine() {

    }

    void go() throws Exception {
        while (1==1) {
            String[] options = new String[] {
                "n) new game",
                "q) quit",
            };

            String cmd = Terminal.menuInteraction(options);
            if (cmd.equals("n")) {
                Game game = new Game();
                game.go();
            }
            else if (cmd.equals("q")) {
                break;
            }
        }
    }
}


public class Main {

    public static void main(String args[]) {
        Terminal.open();
        try {
            Engine engine = new Engine();
            engine.go();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            Terminal.close();
        }
    }
}

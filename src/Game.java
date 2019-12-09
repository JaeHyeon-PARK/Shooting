import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

class Frame_Make extends JFrame implements KeyListener, Runnable {
	int f_width, f_height; // 프레임 가로 및 세로
	int x, y; // 비행기의 좌표값
	int cnt; // 타이밍 조절을 위한 변수
	int bx = 0;
	int player_speed; // 유저의 캐릭터가 움직이는 속도를 조절할 변수
	int missile_speed; // 미사일이 날라가는 속도 조절할 변수
	int fire_speed; // 미사일 연사 속도 조절 변수
	int enemy_speed; // 적 이동 속도 설정
	int game_score; // 게임 점수 계산
	int player_hp; // 플레이어 캐릭터의 체력
	
	boolean KeyUp = false; // 방향키 처리
	boolean KeyDown = false; // 방향키 처리
	boolean KeyLeft = false; // 방향키 처리
	boolean KeyRight = false; // 방향키 처리
	boolean KeySpace = false; // 미사일 발사 처리
	
	Thread th; // 스레드 생성
	Toolkit tk = Toolkit.getDefaultToolkit();
	
	Image me_img;
	Image missile_img;
	Image enemy_img;
	Image bg_img;
	Image ex_img;
	
	ArrayList<Missile> missile_list = new ArrayList<Missile>(); // 다수의 미사일 관리
	ArrayList<Enemy> enemy_list = new ArrayList<Enemy>(); // 다수의 적 관리
	ArrayList<Explosion> explo_list = new ArrayList<Explosion>(); // 다수의 폭발 이펙트 관리
	
	Image buffImage; //더블 버퍼링용
	Graphics buffg; //더블 버퍼링용
	
	Missile ms; // 미사일 클래스 접근
	Enemy en; // 에네미 클래스 접근
	Explosion ex; // 폭발 클래스 접근
	
	public Frame_Make() { // 프레임 만들기
		init();
		start();
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setTitle("Shooting Game");
		setLocation((int)(screen.getWidth() / 2 - f_width / 2), (int)(screen.getHeight() / 2 - f_height / 2));
		setSize(new Dimension(f_width, f_height));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
	}
	
	public void init() {
		x = 100;
		y = 100;
		
		f_width = 1280;
		f_height = 960;
		
		me_img = new ImageIcon("f15k.png").getImage();
		missile_img = new ImageIcon("Missile.png").getImage();
		enemy_img = new ImageIcon("enemy.png").getImage();
		bg_img = new ImageIcon("background.png").getImage();
		ex_img = new ImageIcon("explo.png").getImage();
		
		game_score = 0;
		player_hp = 3;
		player_speed = 5;
		missile_speed = 11;
		fire_speed = 10;
		enemy_speed = 7;
		
		Sound("bg.wav", true);
	}

	public void start() { 
		addKeyListener(this); // 키보드 이벤트
		th = new Thread(this); // 스레드 생성
		th.start(); // 스레드 실행
	}
	
	public void run() { // 스레드 무한 반복
		try {
			while(true) {
				KeyProcess();
				EnemyProcess();
				MissileProcess();
				ExplosionProcess();
				repaint();
				Thread.sleep(20);
				cnt++;
				if(player_hp <= 0) {
					while(true) th.sleep(1000);
				}
			}
		} catch(Exception e) { }
	}

	public void MissileProcess() {
		if(KeySpace) {
			if((cnt % fire_speed) == 0) {
				ms = new Missile(x + 160, y + 30, missile_speed);
				missile_list.add(ms);
			}
		}
	}
	
	public void EnemyProcess() {
		for(int i = 0; i < enemy_list.size(); i++)
			en = enemy_list.get(i);
		
		if(cnt % 150 == 0) { // 일정 주기마다 적을 생성 및 배열 추가
			for(int i = 1; i <= 10; i++) {
				en = new Enemy(f_width + 100, i * 90, enemy_speed);
				enemy_list.add(en);
			}
		}
	}
	
	public void ExplosionProcess() {
		for(int i = 0; i < explo_list.size(); i++) {
			ex = explo_list.get(i);
			ex.effect();
		}
	}
	
	public void paint(Graphics g) {
		buffImage = createImage(f_width, f_height);
		buffg = buffImage.getGraphics();
		update(g);
	}
	
	public void update(Graphics g) {
		Draw_Background();
		buffg.drawImage(me_img, x, y, this);
		Draw_Enemy();
		Draw_Missile();
		Draw_Explosion();
		Draw_Status();
		g.drawImage(buffImage, 0, 0, this);
	}

	public void Draw_Background() {
		buffg.clearRect(0, 0, f_width, f_height);
		if(bx > -3500) {
			buffg.drawImage(bg_img, bx, 0, this);
			bx -= 1;
		}
		else bx = 0;
	}

	public void Draw_Missile() {
		for(int i = 0; i < missile_list.size(); i++) {
			ms = missile_list.get(i);
			buffg.drawImage(missile_img, ms.x, ms.y, this);
			ms.move();
			if(ms.x > f_width - 20)
				missile_list.remove(i);
			for(int j = 0; j < enemy_list.size(); j++) {
				en = enemy_list.get(j);
				if(Crash(ms.x, ms.y, en.x, en.y, missile_img, enemy_img)) {
					missile_list.remove(i);
					enemy_list.remove(j);
					game_score += 10;
					ex = new Explosion(en.x + enemy_img.getWidth(null) / 2, en.y + enemy_img.getHeight(null) / 2);
					explo_list.add(ex);
				}
			}
		}
	}
	
	public void Draw_Enemy() {
		for(int i = 0; i < enemy_list.size(); i++) {
			en = enemy_list.get(i);
			buffg.drawImage(enemy_img, en.x, en.y, this);
			en.move();
			if(en.x < -200)
				enemy_list.remove(i);
			if(Crash(x, y, en.x, en.y, me_img, enemy_img)) {
				player_hp--;
				enemy_list.remove(i);
				game_score += 10;
				ex = new Explosion(en.x + enemy_img.getWidth(null) / 2, en.y + enemy_img.getHeight(null) / 2);
				explo_list.add(ex);
				ex = new Explosion(x + 80, y + 15);
				explo_list.add(ex);
			}
		}
	}
	
	private void Draw_Explosion() {
		for(int i = 0; i < explo_list.size(); i++) {
			ex = explo_list.get(i);
			if(ex.ex_cnt < 7) buffg.drawImage(ex_img, ex.x - ex_img.getWidth(null) / 2, ex.y - ex_img.getHeight(null) / 2, this);
			else {
				explo_list.remove(i);
			}
		}
	}
	
	private void Draw_Status() {
		buffg.setFont(new Font("Defualt", Font.BOLD, 20));
		buffg.drawString("SCORE: " + game_score, 1130, 70);
		buffg.drawString("HP: " + player_hp, 1130, 90);
	}
	
	public boolean Crash(int ms_x, int ms_y, int en_x, int en_y, Image missile, Image enemy) {
		boolean chk = false;
		
		if(Math.abs((ms_x + missile.getWidth(null) / 2) - (en_x + enemy.getWidth(null) / 2)) < (enemy.getWidth(null) / 2 + missile.getWidth(null) / 2) &&
				Math.abs((ms_y + missile.getHeight(null) / 2) - (en_y + enemy.getHeight(null) / 2)) < (enemy.getHeight(null) / 2 + missile.getHeight(null) / 2))
			chk = true;
		else chk = false;
		
		return chk;
	}
	
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == 32) KeySpace = true;
		if(e.getKeyCode() == 37) KeyLeft = true;
		if(e.getKeyCode() == 38) KeyUp = true;
		if(e.getKeyCode() == 39) KeyRight = true;
		if(e.getKeyCode() == 40) KeyDown = true;
		
	}

	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == 32) KeySpace = false;
		if(e.getKeyCode() == 37) KeyLeft = false;
		if(e.getKeyCode() == 38) KeyUp = false;
		if(e.getKeyCode() == 39) KeyRight = false;
		if(e.getKeyCode() == 40) KeyDown = false;
	}
	
	public void keyTyped(KeyEvent e) { }
	
	public void KeyProcess() {
		if(KeyUp == true) {
			if(y > 20) y -= player_speed;
		}
		if(KeyDown == true) {
			if(y + me_img.getHeight(null) < f_height) y += player_speed;
		}
		if(KeyLeft == true) {
			if(x > 0) x -= player_speed;
		}
		if(KeyRight == true) {
			if(x + me_img.getWidth(null) < f_width) x += player_speed;
		}
	}
	
	public void Sound(String file, boolean loop) {
		Clip clip;
		
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
			clip = AudioSystem.getClip();
			clip.open(ais);
			clip.start();
			if(loop) clip.loop(-1);
		} catch(Exception e) { e.printStackTrace(); }
	}
}

class Missile {
	int x, y, speed;
	
	public Missile(int x, int y, int speed) {
		this.x = x;
		this.y = y;
		this.speed = speed;
	}
	
	public void move() {
		x += speed;
	}
}

class Enemy {
	int x, y, speed;
	
	public Enemy(int x, int y, int speed) {
		this.x = x;
		this.y = y;
		this.speed = speed;
	}
	
	public void move() {
		x -= speed;
	}
}

class Explosion {
	int x, y, ex_cnt;
	
	public Explosion(int x, int y) {
		this.x = x;
		this.y = y;
		this.ex_cnt = 0;
	}
	
	public void effect() {
		ex_cnt++;
	}
}

public class Game {
	public static void main(String[] args) {
		Frame_Make frameMake = new Frame_Make();
	}
}
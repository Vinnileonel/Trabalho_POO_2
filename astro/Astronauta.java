package jetpac.astro;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import jetpac.drag.*;
import jetpac.mundo.WorldElementDefault;
import prof.jogos2D.image.ComponenteMultiAnimado;
import prof.jogos2D.util.Vector2D;

/**
 * Esta classe representa o astronauta do jogo. Pode andar, disparar, carregar
 * items, etc
 */
public class Astronauta extends WorldElementDefault {

	private static final int VELOCIDADE_VOO = 6;
	private static final int VELOCIDADE_ANDAR = 4;

	// as direções do jogo (podiam estar numa enumeração)
	public static final int RIGHT = 0;
	public static final int LEFT = 1;

	private Point initialPos; // posição inicial do astronauta
	private int dir; // direção atual
	private boolean jetPacOn = false; // tem o jetpac ligado?
	private boolean rising = false; // está a subir?
	private boolean shooting = false; // está a disparar?
	private boolean walking = false; // está a andar?
	private boolean dead = false; // está morto?

	private final int shootSpeed = 4; // velocidade de disparo
	private int nextShot = 0; // temporizador de disparo
	private int rangeIdx = 0; // indíce do alcance do laser
	private int offsetDispY; // ponta da arma relativa à imagem do astronauta

	// alcances do laser para dar um "efeito especial"
	private int ranges[] = { 150, 300, 450, 600, 650, 700 };

	// indicação do que está a carregar
	// TODO serão precisas tantas variáveis?
	private Fuel fuel;
	private Tesouro tesouro;
	private SpaceshipPart parte;

	/**
	 * Cria o astronauta
	 * 
	 * @param image    imagem representativa do astronauta
	 * @param pos      posição inicial
	 * @param dir      direção inical (esquerda ou direita)
	 * @param offShoot localização da arma relativa à imagem
	 */
	public Astronauta(ComponenteMultiAnimado image, Point pos, int dir, int offShoot) {
		super(image);
		initialPos = (Point) pos.clone();
		image.setPosicao(pos);
		this.dir = dir;
		offsetDispY = offShoot;
	}

	/**
	 * Cria o astronauta, colocando-o virado para a esquerda
	 * 
	 * @param image    imagem representativa do astronauta
	 * @param pos      posição inicial
	 * @param offShoot localização da arma relativa à imagem
	 */
	public Astronauta(ComponenteMultiAnimado image, Point pos, int offShoot) {
		this(image, pos, LEFT, offShoot);
	}

	public int getDirection() {
		return dir;
	}

	public void setDirection(int dir) {
		this.dir = dir;
	}

	public void setJetPacOn(boolean v) {
		jetPacOn = v;
	}

	public boolean isJetPacOn() {
		return jetPacOn;
	}

	public void setRising(boolean s) {
		rising = s;
	}

	public boolean isRising() {
		return rising;
	}

	public boolean isShooting() {
		return shooting;
	}

	public void setShooting(boolean s) {
		shooting = s;
		if (!shooting)
			rangeIdx = 0;
	}

	public void setWalking(boolean andar) {
		this.walking = andar;
	}

	@Override
	public void draw(Graphics2D g) {
		ComponenteMultiAnimado img = (ComponenteMultiAnimado) getImage();
		if (img.getAnim() != 4) {
			int anim = getDirection() == LEFT ? 0 : 1;
			if (!jetPacOn)
				anim += 2;
			img.setAnim(anim);
			if (!walking && !jetPacOn)
				img.setFrameNum(0);
		}
		super.draw(g);
	}

	/**
	 * indica se está a arrastar qualquer coisa
	 * 
	 * @return true se está a arrastar qualquer coisa
	 */
	public boolean isDragging() {
		// TODO serão precisas tantas variáveis?
		return fuel != null || tesouro != null || parte != null;
	}

	@Override
	public void update() {
		// se está morto não faz nada
		if (dead)
			return;

		// ver que movimento está a fazer
		int dy = rising ? -VELOCIDADE_VOO : VELOCIDADE_VOO;
		if (walking && rising)
			move((dir == LEFT ? -VELOCIDADE_VOO : VELOCIDADE_VOO), dy);
		else if (walking)
			move((dir == LEFT ? -VELOCIDADE_ANDAR : VELOCIDADE_ANDAR), dy);
		else
			move(0, dy);

		if (shooting)
			fire();

		setJetPacOn(true);

		Rectangle astroBounds = getBounds();
		for (Plataforma p : getWorld().getPlatforms()) {
			Vector2D v = p.hitV(astroBounds);
			if (v.x == 0 && v.y == 0)
				continue;
			move((int) v.x, (int) v.y);
			// se bateu em cima, desligar o jetpac porque pousou
			if (v.x == 0 && v.y < 0) {
				setJetPacOn(false);
			}
		}

		// se a nave está cheia verificar se bate na nave para passar o nível
		if (getWorld().getFuelPercentage() >= 100) {
			Spaceship n = getWorld().getMainSpaceship();
			if (n.getBounds().intersects(getBounds())) {
				getWorld().completed();
				setPosition(new Point(-100, -100)); // esconder o astronauta
			}
		}
	}

	@Override
	public void move(int dx, int dy) {
		if (getImage().getPosicao().y + dy < 0)
			dy = -getImage().getPosicao().y;

		if (getImage().getPosicao().x < 0)
			dx += getWorld().getWidth();
		else if (getImage().getPosicao().x > getWorld().getWidth())
			dx -= getWorld().getWidth();

		super.move(dx, dy);
	}

	/**
	 * larga o que está a carregar
	 */
	public void drop() {
		if (!isDragging())
			return;
		if (fuel != null)
			fuel.release();
		if (tesouro != null)
			tesouro.release();
		if (parte != null)
			parte.release();

		// já não tem nada a arrastar
		fuel = null;
		tesouro = null;
		parte = null;
	}

	/**
	 * dispara
	 */
	private void fire() {
		// se já chegou a altura de disparar
		if (nextShot <= 0) {
			// definir o alcance deste tiro
			rangeIdx++;
			if (rangeIdx >= ranges.length)
				rangeIdx = 0;
			int x = dir == LEFT ? getPosition().x : getPosition().x + getImage().getComprimento();
			int y = getPosition().y + offsetDispY;

			// criar e adicionar o laser ao mundo
			getWorld().addLaser(new Laser(new Point(x, y), dir == LEFT ? -ranges[rangeIdx] : ranges[rangeIdx]));

			// reinicializar o contador
			nextShot = shootSpeed;
		} else
			nextShot--;
	}

	/**
	 * "mata" o astronauta
	 */
	public void die() {
		if (dead)
			return;

		// selecionar a animação de morte
		ComponenteMultiAnimado img = (ComponenteMultiAnimado) getImage();
		img.setAnim(4);
		img.setFrameNum(0);
		img.setCiclico(false);
		dead = true;

		// avisar o mundo da "morte do artista"
		getWorld().dying();

		// se está a carregar tem de soltar a carga
		drop();
	}

	/**
	 * informa se está completamente morto. Só está completamente morto se a
	 * animação de morte estiver concluida
	 * 
	 * @return true se o astronauta está morto
	 */
	public boolean isDead() {
		ComponenteMultiAnimado img = (ComponenteMultiAnimado) getImage();
		return dead && img.numCiclosFeitos() > 1;
	}

	/**
	 * "ressuscita" o astronauta
	 */
	public void reset() {
		setPosition((Point) initialPos.clone());
		dead = false;
		// coloca a animação no início
		ComponenteMultiAnimado img = (ComponenteMultiAnimado) getImage();
		img.setAnim(0);
		img.setCiclico(true);
	}

	public void pickFuel(Fuel f) {
		fuel = f;
	}

	public void pickTesouro(Tesouro t) {
		tesouro = t;
	}

	public void pickParte(SpaceshipPart p) {
		parte = p;
	}
}

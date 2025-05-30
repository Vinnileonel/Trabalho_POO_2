package jetpac.drag;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import jetpac.astro.Astronauta;
import jetpac.astro.Plataforma;
import jetpac.mundo.Mundo;
import prof.jogos2D.image.ComponenteVisual;
import prof.jogos2D.util.ReguladorVelocidade;
import prof.jogos2D.util.Vector2D;

/**
 * classe que prepresenta um tesouro que aparece no mundo. Os tesouros aumentam
 * a pontuação do jogador, mas, se não forem apanhados, desaparecem após algum
 * tempo
 * 
 */
public class Tesouro {

	private long lifeTime; // tempo de vida
	private int score; // pontuação
	private long lifeLeft;

	/** Tempo que demora até poder pegar outra vez no objeto */
	private static final int TEMPO_ENTRE_APANHADAS = 30;

	enum State {
		FALLING, REST, DRAGGED, DROPING, DELIVERED
	}

	private State estado = State.FALLING;
	private Mundo world;
	private ComponenteVisual imagem;
	private int offsetX, offsetY; // offset da posição do astronauta quando a ser arrastado
	private int nextDrag = 0;

	/**
	 * Cria um tesouro
	 * 
	 * @param p        posição onde vai aparecer
	 * @param lifeTime tempo de vida
	 * @param score    pontuação
	 * @param img      imagem
	 */
	public Tesouro(Point p, int lifeTime, int score, ComponenteVisual img) {
		this.imagem = img;
		imagem.setPosicao(p);
		this.lifeLeft = lifeTime;
		this.lifeTime = ReguladorVelocidade.tempoRelativo() + lifeTime;
		this.score = score;
	}

	public void draw(Graphics2D g) {
		imagem.desenhar(g);
	}

	private Rectangle getBounds() {
		return imagem.getBounds();
	}

	public boolean isActive() {
		return estado != State.DELIVERED && lifeLeft > 0;
	}

	public boolean isDraggable() {
		return (estado == State.FALLING || estado == State.REST) && nextDrag == 0;
	}

	public boolean isFalling() {
		return estado == State.FALLING;
	}

	private void move(int dx, int dy) {
		imagem.getPosicao().translate(dx, dy);
	}

	public void release() {
		setEstado(State.FALLING);
		nextDrag = TEMPO_ENTRE_APANHADAS;
	}

	public void setWorld(Mundo w) {
		world = w;
	}

	public Mundo getWorld() {
		return world;
	}

	public void update() {
		// ver se já passou o tempo de validade
		if (getEstado() != State.DRAGGED && getEstado() != State.DROPING) {
			if (lifeTime < ReguladorVelocidade.tempoRelativo()) {
				lifeLeft = 0;
				getWorld().getTreasureGenerator().treasureRemoved();
			}
		}
		Astronauta astronauta = getWorld().getAstronaut();
		if (estado == State.DRAGGED) {
			updatePosicao(astronauta);
			testDropZone(astronauta);
			return;
		}
		if (nextDrag > 0)
			nextDrag--;

		if (estado == State.FALLING || estado == State.DROPING) {
			updateFall();
		}

		verSeFoiApanhado(astronauta);

		// já chegou à nave?
		if (getWorld().getMainSpaceship().getBounds().intersects(getBounds())) {
			delivered();
		}
	}

	private void verSeFoiApanhado(Astronauta astronauta) {
		// se já tem alguma coisa ou esta não é apanhável, não apanha
		if (!astronauta.isDragging() && isDraggable()) {
			if (astronauta.getBounds().intersects(imagem.getBounds())) {
				apanhado(astronauta);
			}
		}
	}

	private void apanhado(Astronauta astronauta) {
		Rectangle astroBounds = astronauta.getBounds();
		astronauta.pickTesouro(this);
		offsetX = (astroBounds.width - getBounds().width) / 2;
		offsetY = (astroBounds.height - getBounds().height);
		setEstado(State.DRAGGED);
	}

	private void updateFall() {
		move(0, 2);
		for (Plataforma p : getWorld().getPlatforms()) {
			Vector2D toqueV = p.hitV(getBounds());
			if (toqueV.x != 0 || toqueV.y != 0) {
				move((int) toqueV.x, (int) toqueV.y);
				estado = State.REST;
			}
		}
	}

	/**
	 * atualiza a posição em função do astronauta
	 * 
	 * @param astronauta o astronauta
	 */
	private void updatePosicao(Astronauta astronauta) {
		Point pos = astronauta.getPosition();
		imagem.setPosicao(new Point(pos.x + offsetX, pos.y + offsetY));
	}

	/**
	 * Verifica se ao ser arrastada pelo astronauta está sobre a drop zone
	 * 
	 * @param astronauta o astronauta
	 */
	private void testDropZone(Astronauta astronauta) {
		// verificar se está na drop zone
		Rectangle dropArea = getWorld().getMainSpaceship().getDropArea();
		if (dropArea.intersects(getBounds())) {
			astronauta.drop();
			setEstado(State.DROPING);
			// centrar em relação à dropArea
			Point oldPos = imagem.getPosicaoCentro();
			imagem.setPosicaoCentro(new Point(dropArea.x + dropArea.width / 2, oldPos.y));
		}
	}

	private State getEstado() {
		return estado;
	}

	private void setEstado(State estado) {
		this.estado = estado;
		if (estado == State.DRAGGED) {
			lifeLeft = lifeTime - ReguladorVelocidade.tempoRelativo();
		} else {
			lifeTime = lifeLeft + ReguladorVelocidade.tempoRelativo();
		}
	}

	public ComponenteVisual getImagem() {
		return imagem;
	}

	/**
	 * devolve a pontuação do tesouro
	 * 
	 * @return a pontuação do tesouro
	 */
	public int getScore() {
		return score;
	}

	protected void delivered() {
		setEstado(State.DELIVERED);
		lifeLeft = 0;
		getWorld().addCiclePoints(getScore());
		getWorld().getTreasureGenerator().treasureRemoved();
	}
}

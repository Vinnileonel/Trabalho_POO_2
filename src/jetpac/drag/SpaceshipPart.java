package jetpac.drag;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import jetpac.astro.Astronauta;
import jetpac.astro.Plataforma;
import jetpac.mundo.Mundo;
import prof.jogos2D.image.ComponenteVisual;
import prof.jogos2D.util.Vector2D;

/**
 * Classe que representa as partes da nave. Cada parte tem uma ordem em que deve
 * ser colocada na nave
 */
public class SpaceshipPart {

	private int partIdx; // ordem da parte da nave

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
	 * Cria uma parte da nave
	 * 
	 * @param partIdx o índice desta parte
	 * @param pos     posição onde aparece a parte
	 * @param img     imagem da parte
	 */
	public SpaceshipPart(int partIdx, Point pos, ComponenteVisual img) {
		this.imagem = img;
		imagem.setPosicao(pos);
		this.partIdx = partIdx;
	}

	public void draw(Graphics2D g) {
		imagem.desenhar(g);
	}

	private Rectangle getBounds() {
		return imagem.getBounds();
	}

	public boolean isActive() {
		return estado != State.DELIVERED;
	}

	public boolean isDraggable() {
		return (estado == State.FALLING || estado == State.REST) && nextDrag == 0
				&& partIdx == getWorld().getMainSpaceship().getNextPartDue();
	}

	public boolean isFalling() {
		return estado == State.FALLING;
	}

	private void move(int dx, int dy) {
		imagem.getPosicao().translate(dx, dy);
	}

	public void release() {
		estado = State.FALLING;
		nextDrag = TEMPO_ENTRE_APANHADAS;
	}

	public void setWorld(Mundo w) {
		world = w;
	}

	public Mundo getWorld() {
		return world;
	}

	public void update() {
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
		astronauta.pickParte(this);
		offsetX = (astroBounds.width - getBounds().width) / 2;
		offsetY = (astroBounds.height - getBounds().height);
		estado = State.DRAGGED;
	}

	private void updateFall() {
		move(0, 2);
		for (Plataforma p : getWorld().getPlatforms()) {
			Vector2D toqueV = p.hitV(getBounds());
			// se bateu em algum lado pára
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
			estado = State.DROPING;
			// centrar em relação à dropArea
			Point oldPos = imagem.getPosicaoCentro();
			imagem.setPosicaoCentro(new Point(dropArea.x + dropArea.width / 2, oldPos.y));
		}
	}

	protected State getEstado() {
		return estado;
	}

	protected void setEstado(State estado) {
		this.estado = estado;
	}

	public ComponenteVisual getImagem() {
		return imagem;
	}

	/**
	 * indica o número da parte da nave
	 * 
	 * @return o número da parte da nave
	 */
	public int getPartIdx() {
		return partIdx;
	}

	private void delivered() {
		estado = State.DELIVERED;
		getWorld().getMainSpaceship().addParte(this);
	}
}

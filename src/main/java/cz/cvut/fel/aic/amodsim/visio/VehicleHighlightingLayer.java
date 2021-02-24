/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.amodsim.visio;

import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.vecmath.Point2d;

public class VehicleHighlightingLayer extends AbstractLayer {

	private static final int UI_WIDTH = 300;
	private static final int UI_HEIGHT = 100;

	private int uixposition;
	private int uiyposition;
	private boolean acceptingInput;

	private Vis vis;
	private Color textColor;
	private Color backgroundColor;
	private Color acceptingTextColor;
	private String input;
	private String status;
	private KeyListener keyListener;
	private MouseListener mouseListener;
	private OnDemandVehicleLayer vehicleLayer;


	public VehicleHighlightingLayer() {
		setDefaultColors();
		vehicleLayer = null;
		status = "No vehicle highlighted.";
		input = "";
		acceptingInput = false;
	}

	public void setVehicleLayer(OnDemandVehicleLayer vehicleLayer) {
		this.vehicleLayer = vehicleLayer;
	}

	@Override
	public void paint(Graphics2D canvas) {
		Dimension dimension = Vis.getDrawingDimension();
		uixposition = dimension.width - UI_WIDTH - 30;
		uiyposition = dimension.height - UI_HEIGHT - 400;

		canvas.setColor(backgroundColor);
		canvas.fillRect(uixposition, uiyposition, 300, 100);

		Font oldFont = canvas.getFont();
		canvas.setFont(new Font("Arial", 1, 14));
		canvas.setColor(textColor);
		canvas.drawString("Vehicle ID to highlight:", uixposition+10, uiyposition+20);
		canvas.drawString("Status: " + status, uixposition+10, uiyposition+90);

		canvas.setStroke(new BasicStroke(2));
		canvas.drawRect(uixposition+10, uiyposition+30, 135, 40);
		canvas.drawRect(uixposition+155, uiyposition+30, 135, 40);
		if (acceptingInput) {
			canvas.setColor(acceptingTextColor);
			canvas.setStroke(new BasicStroke(1));
			canvas.fillRect(uixposition+11, uiyposition+31, 133,38);
			canvas.setColor(textColor);
		}

		canvas.setFont(new Font("Arial", 1, 18));
		canvas.drawString("HIGHLIGHT", uixposition + 165, uiyposition + 57);
		canvas.drawString(input, uixposition + 20, uiyposition + 57);



		canvas.setFont(oldFont);
	}

	public void init(Vis vis) {
		super.init(vis);
		this.vis = vis;
		this.mouseListener = getMouseListener();
		this.keyListener = getKeyListener();
		vis.addMouseListener(this.mouseListener);
	}

	public void deinit(Vis vis) {
		super.deinit(vis);
		vis.removeMouseListener(this.mouseListener);
	}

	public MouseListener getMouseListener() {
		return new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
					Point2d click = new Point2d(mouseEvent.getX(), mouseEvent.getY());

					if (click.getX() > uixposition+155 && click.getX() < uixposition+290 && click.getY() > uiyposition+30 && click.getY() < uiyposition+70) {
						status = "Highlighting vehicle '" + input + "'.";
						vehicleLayer.setHighlightedID(input);
					}

					if (click.getX() > uixposition+10 && click.getX() < uixposition+145 && click.getY() > uiyposition+30 && click.getY() < uiyposition+70) {
						if (acceptingInput == false ) {
							vis.addKeyListener(keyListener);
							acceptingInput = true;
						}
						return;
					}

					if (acceptingInput == true ) {
						vis.removeKeyListener(keyListener);
						acceptingInput = false;
					}

				}
			}

			@Override
			public void mouseEntered(MouseEvent me) {

			}

			@Override
			public void mouseExited(MouseEvent me) {

			}

			@Override
			public void mousePressed(MouseEvent me) {

			}

			@Override
			public void mouseReleased(MouseEvent me) {

			}
		};
	}

	public KeyListener getKeyListener() {
		return new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				int pressedKey = e.getKeyCode();
				if (pressedKey == KeyEvent.VK_BACK_SPACE) {
					if (input.length() > 0 ) {
						input = input.substring(0, input.length()-1);
					}
				} else if (pressedKey != KeyEvent.VK_ALT && pressedKey != KeyEvent.VK_SHIFT && pressedKey != KeyEvent.VK_ENTER ) {
					input += e.getKeyChar();
				}

			}
		};
	}

	private void setDefaultColors() {
		backgroundColor = new Color(0, 0, 0, 170);
		textColor = new Color(255, 255, 255, 255);
		acceptingTextColor = new Color(173, 173, 173, 170);
	}

}

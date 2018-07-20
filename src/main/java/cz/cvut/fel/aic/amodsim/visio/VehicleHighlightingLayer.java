package cz.cvut.fel.aic.amodsim.visio;

import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;

import javax.swing.*;
import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class VehicleHighlightingLayer extends AbstractLayer {
    private Color backgroundColor;
    private Color textColor;
    private Color acceptingTextColor;
    private int uixposition;
    private int uiyposition;
    private OnDemandVehicleLayer vehicleLayer;
    private String status;
    private MouseListener mouseListener;
    private KeyListener keyListener;
    private String input;
    private boolean acceptingInput;
    private Vis vis;


    public VehicleHighlightingLayer() {
        setDefaultColors();
        setDefaultUIposition();
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
                    input = input.substring(0, input.length()-1);
                } else if (pressedKey != KeyEvent.VK_ALT && pressedKey != KeyEvent.VK_SHIFT) {
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

    private void setDefaultUIposition() {
        uixposition = 1580;
        uiyposition = 500;
    }


}

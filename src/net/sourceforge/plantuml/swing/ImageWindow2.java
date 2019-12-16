/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2020, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * http://plantuml.com/patreon (only 1$ per month!)
 * http://plantuml.com/paypal
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 *
 *
 */
package net.sourceforge.plantuml.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.JSVGScrollPane;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRectElement;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.ImageSelection;
import net.sourceforge.plantuml.graphic.GraphicStrings;
import net.sourceforge.plantuml.svek.TextBlockBackcolored;
import net.sourceforge.plantuml.ugraphic.ColorMapperIdentity;
import net.sourceforge.plantuml.ugraphic.ImageBuilder;
import net.sourceforge.plantuml.version.PSystemVersion;

class ImageWindow2 extends JFrame {

	private static final double ZOOM_IN_SCALE = -0.1;
	private static final double ZOOM_OUT_SCALE = 0.1;
	private final static Preferences prefs = Preferences.userNodeForPackage(ImageWindow2.class);
	private final static String KEY_ZOOM_FIT = "zoomfit";
	private final static String KEY_WIDTH_FIT = "widthfit";

	private SimpleLine2 simpleLine2;
	private final JScrollPane scrollPane;
	private final JButton next = new JButton("Next");
	private final JButton copy = new JButton("Copy");
	private final JButton previous = new JButton("Previous");
	private final JCheckBox zoomFitButt = new JCheckBox("Zoom fit");
	private final JCheckBox widthFitButt = new JCheckBox("Width fit");
	private final JButton zoomMore = new JButton("+");
	private final JButton zoomLess = new JButton("-");
	private final MainWindow2 main;

	private final ListModel listModel;
	private int index;
	private int zoomFactor = 0;
	private JSVGScrollPane svgPanel;
	private double hwRate;

	private enum SizeMode {
		FULL_SIZE, ZOOM_FIT, WIDTH_FIT
	};

	private SizeMode sizeMode = SizeMode.FULL_SIZE;

	private int startX, startY;
	private boolean mousePressed;

	public ImageWindow2(SimpleLine2 simpleLine, final MainWindow2 main, ListModel listModel, int index) {
		super(simpleLine.toString());
		setIconImage(PSystemVersion.getPlantumlSmallIcon2());
		this.simpleLine2 = simpleLine;
		this.listModel = listModel;
		this.index = index;
		this.main = main;

		final JPanel north = new JPanel();
		north.add(previous);
		north.add(copy);
		north.add(next);
		north.add(zoomFitButt);
		north.add(widthFitButt);
		north.add(zoomMore);
		north.add(zoomLess);
		copy.setFocusable(false);
		copy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				copy();
			}
		});
		next.setFocusable(false);
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				next();
			}
		});
		previous.setFocusable(false);
		previous.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				previous();
			}
		});
		zoomFitButt.setFocusable(false);
		zoomFitButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				widthFitButt.setSelected(false);
				zoomFit();
			}
		});
		widthFitButt.setFocusable(false);
		widthFitButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				zoomFitButt.setSelected(false);
				zoomFit();
			}
		});
		zoomMore.setFocusable(false);
		zoomMore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				zoomFactor++;
				refreshImage(false);
			}
		});
		zoomLess.setFocusable(false);
		zoomLess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				zoomFactor--;
				refreshImage(false);
			}
		});

		scrollPane = new JScrollPane(buildScrollablePicture());
		getContentPane().add(north, BorderLayout.NORTH);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		setSize(600, 400);
		this.setLocationRelativeTo(this.getParent());
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				main.closing(ImageWindow2.this);
			}
		});

		this.addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent e) {
				super.componentResized(e);
				refreshImage(false);
			}
		});

		final boolean zoomChecked = prefs.getBoolean(KEY_ZOOM_FIT, false);
		zoomFitButt.setSelected(zoomChecked);
		if (zoomChecked) {
			sizeMode = SizeMode.ZOOM_FIT;
		}
		final boolean widthZoomChecked = prefs.getBoolean(KEY_WIDTH_FIT, false);
		widthFitButt.setSelected(widthZoomChecked);
		if (widthZoomChecked) {
			sizeMode = SizeMode.WIDTH_FIT;
		}

		this.setFocusable(true);
		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent evt) {
				if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_RIGHT) {
					next();
				} else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_LEFT) {
					previous();
				} else if (evt.isAltDown() && evt.getKeyCode() == KeyEvent.VK_RIGHT) {
					next();
				} else if (evt.isAltDown() && evt.getKeyCode() == KeyEvent.VK_LEFT) {
					previous();
				} else if (evt.getKeyCode() == KeyEvent.VK_RIGHT) {
					imageRight();
				} else if (evt.getKeyCode() == KeyEvent.VK_LEFT) {
					imageLeft();
				} else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
					imageDown();
				} else if (evt.getKeyCode() == KeyEvent.VK_UP) {
					imageUp();
				} else if (evt.getKeyCode() == KeyEvent.VK_C) {
					copy();
				} else if (evt.getKeyCode() == KeyEvent.VK_Z) {
					zoomFitButt.setSelected(!zoomFitButt.isSelected());
					zoomFit();
				}
			}
		});

	}

	private void next() {
		index++;
		updateSimpleLine();
	}

	private void previous() {
		index--;
		updateSimpleLine();
	}

	private void imageDown() {
		final JScrollBar bar = scrollPane.getVerticalScrollBar();
		bar.setValue(bar.getValue() + bar.getBlockIncrement());
	}

	private void imageUp() {
		final JScrollBar bar = scrollPane.getVerticalScrollBar();
		bar.setValue(bar.getValue() - bar.getBlockIncrement());
	}

	private void imageLeft() {
		final JScrollBar bar = scrollPane.getHorizontalScrollBar();
		bar.setValue(bar.getValue() - bar.getBlockIncrement());
	}

	private void imageRight() {
		final JScrollBar bar = scrollPane.getHorizontalScrollBar();
		bar.setValue(bar.getValue() + bar.getBlockIncrement());
	}

	private void zoomFit() {
		final boolean selectedZoom = zoomFitButt.isSelected();
		final boolean selectedWidth = widthFitButt.isSelected();
		prefs.putBoolean(KEY_ZOOM_FIT, selectedZoom);
		prefs.putBoolean(KEY_WIDTH_FIT, selectedWidth);
		zoomFactor = 0;
		if (selectedZoom) {
			sizeMode = SizeMode.ZOOM_FIT;
		} else if (selectedWidth) {
			sizeMode = SizeMode.WIDTH_FIT;
		} else {
			sizeMode = SizeMode.FULL_SIZE;
		}
		refreshImage(false);
	}

	private void updateSimpleLine() {
		if (index < 0) {
			index = 0;
		}
		if (index > listModel.getSize() - 1) {
			index = listModel.getSize() - 1;
		}
		simpleLine2 = (SimpleLine2) listModel.getElementAt(index);
		setTitle(simpleLine2.toString());
		refreshImage(false);
	}

	private void refreshSimpleLine() {
		for (SimpleLine2 line : main.getCurrentDirectoryListing2()) {
			if (line.getFile().equals(simpleLine2.getFile())) {
				simpleLine2 = line;
				setTitle(simpleLine2.toString());
			}
		}
	}

	private JComponent buildScrollablePicture() {
		final GeneratedImage generatedImage = simpleLine2.getGeneratedImage();
		if (generatedImage == null) {
			return null;
		}
		final File png = generatedImage.getImage();
		if (png.getName().toLowerCase().endsWith("svg")) {
			return buildScrollablePictureBySvg(png);
		} else {
			return buildScrollablePictureByPng(png);
		}

	}

	private JSVGCanvas buildScrollablePictureBySvg(File svg) {
		if (svgCanvas == null) {
			svgCanvas = new JSVGCanvas();
			// svgPanel = new JSVGScrollPane(svgCanvas);
			// svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
			svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_INTERACTIVE);
			svgCanvas.setEnableImageZoomInteractor(true);
			svgCanvas.setEnablePanInteractor(true);
			// svgCanvas.setEnableRotateInteractor(false);
			svgCanvas.setEnableZoomInteractor(true);
			MouseAdapter mouseListener = new MouseAdapter() {
				private int sX, sY;
				boolean mousePressed;

				@Override
				public void mouseReleased(MouseEvent e) {
					mousePressed = false;
				}

				@Override
				public void mousePressed(MouseEvent e) {
					mousePressed = true;
					Point point = e.getPoint();

					System.out.println("mousePressed at " + point);

					sX = point.x;

					sY = point.y;
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					Point p = e.getPoint();
					AffineTransform at = new AffineTransform();
					at.translate(p.x - sX, p.y - sY);
					at.concatenate(svgCanvas.getRenderingTransform());
					if (at.getScaleX() > 0.1) {
						svgCanvas.setRenderingTransform(at);
					}
					sX = p.x;

					sY = p.y;
				}

			};
			svgCanvas.addMouseListener(mouseListener);
			svgCanvas.addMouseMotionListener(mouseListener);
			svgCanvas.addMouseWheelListener(new MouseWheelListener() {
				private boolean isWheelDown(final MouseWheelEvent e) {
					return e.getUnitsToScroll() > 0;
				}

				@Override
				public void mouseWheelMoved(final MouseWheelEvent e) {

					UpdateManager updateManager = svgCanvas.getUpdateManager();
					if (updateManager != null) {
						updateManager.getUpdateRunnableQueue().invokeLater(new Runnable() {
							public void run() {
								double scale = isWheelDown(e) ? ZOOM_OUT_SCALE : ZOOM_IN_SCALE;
								double tx = isWheelDown(e) ? 100 : -100;
								double ty = isWheelDown(e) ? 100 : -100;
								AffineTransform at = new AffineTransform();
								if (e.isControlDown()) {
									at.translate(0, -ty);
								} else if (e.isShiftDown()) {
									at.translate(-tx, 0);
								} else {
									at.translate(tx, 0);
									at.scale(1 - scale, 1 - scale);
								}
								at.concatenate(svgCanvas.getRenderingTransform());
								if (at.getScaleX() > 0.1) {
									svgCanvas.setRenderingTransform(at);
								}
							}
						});
					}
					e.consume();
				}
			});
			svgCanvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {
				private void installMouseClickHandler(Node node) {
					if (node.getNodeName().contains("rect") && node instanceof EventTarget) {
						((EventTarget) node).addEventListener("click", new EventListener() {
							@Override
							public void handleEvent(Event evt) {
								SVGRectElement element = ((SVGRectElement) evt.getCurrentTarget());
								for (int index = 0; index < element.getAttributes().getLength(); index++) {
									System.out.println(String.format("Attribute %s: %s",
											element.getAttributes().item(index).getNodeName(),
											element.getAttributes().item(index).getNodeValue()));
								}
							}
						}, false);
						// ((EventTarget) node).addEventListener("mouseover", new EventListener() {
						// @Override
						// public void handleEvent(Event evt) {
						// System.out.println("Mouse is overing.");
						// }
						// }, false);
					}
					for (int index = 0; index < node.getChildNodes().getLength(); index++) {
						installMouseClickHandler(node.getChildNodes().item(index));
					}
				}

				public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
					SVGDocument doc = svgCanvas.getSVGDocument();
					if (doc != null) {
						installMouseClickHandler(doc);
						String width = doc.getRootElement().getAttribute("width");
						String height = doc.getRootElement().getAttribute("height");
						if (width.endsWith("px")) {
							width = width.substring(0, width.length() - 2);
						}
						if (height.endsWith("px")) {
							height = height.substring(0, height.length() - 2);
						}
						hwRate = Double.parseDouble(height) / Double.parseDouble(width);

						Dimension dimension = getScreenSize(ImageWindow2.this);

						double maxWidth = dimension.getWidth() - 200;
						double maxHeight = dimension.getHeight() - 100;

						int newHeight = (int) Math.min(maxHeight, Integer.parseInt(height) + 100);
						int newWidth = (int) (newHeight / hwRate);
						if (newWidth > maxWidth) {
							newWidth = (int) Math.min(Integer.parseInt(width) + 150, maxWidth);
							newHeight = (int) (newWidth * hwRate);
						}
						assert newWidth <= maxWidth && newHeight <= maxHeight;
						ImageWindow2.this.setBounds((int) (dimension.getWidth() - newWidth) / 2,
								(int) (dimension.getHeight() - newHeight) / 2, newWidth, newHeight);
						ImageWindow2.this.invalidate();
					}
				}
			});
			try {
				svgCanvas.setURI(svg.toURI().toURL().toString());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return svgCanvas;
	}

	public static Dimension getScreenSize(Window window) {
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = screensize.width;
		int h = screensize.height;

		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(window.getGraphicsConfiguration());

		w = w - (screenInsets.left + screenInsets.right);
		h = h - (screenInsets.top + screenInsets.bottom);

		return new Dimension(w, h);
	}

	private ScrollablePicture buildScrollablePictureByPng(File png) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(png.getAbsolutePath()));
			if (sizeMode == SizeMode.ZOOM_FIT) {
				final Dimension imageDim = new Dimension(image.getWidth(), image.getHeight());
				final Dimension newImgDim = ImageHelper.getScaledDimension(imageDim,
						scrollPane.getViewport().getSize());
				image = ImageHelper.getScaledInstance(image, newImgDim, getHints(), true);
			} else if (sizeMode == SizeMode.WIDTH_FIT) {
				final Dimension imageDim = new Dimension(image.getWidth(), image.getHeight());
				final Dimension newImgDim = ImageHelper.getScaledDimensionWidthFit(imageDim,
						scrollPane.getViewport().getSize());
				image = ImageHelper.getScaledInstance(image, newImgDim, getHints(), false);
			} else if (zoomFactor != 0) {
				final Dimension imageDim = new Dimension(image.getWidth(), image.getHeight());
				final Dimension newImgDim = ImageHelper.getScaledDimension(imageDim, getZoom());
				image = ImageHelper.getScaledInstance(image, newImgDim, getHints(), false);
			}
		} catch (IOException ex) {
			final String msg = "Error reading file: " + ex.toString();
			final TextBlockBackcolored error = GraphicStrings.createForError(Arrays.asList(msg), false);
			final ImageBuilder imageBuilder = new ImageBuilder(new ColorMapperIdentity(), 1.0, error.getBackcolor(),
					null, null, 0, 0, null, false);
			imageBuilder.setUDrawable(error);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				imageBuilder.writeImageTOBEMOVED(new FileFormatOption(FileFormat.DEFAULT_FORMAT), 42, baos);
				baos.close();
				image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		final ImageIcon imageIcon = new ImageIcon(image, simpleLine2.toString());
		final ScrollablePicture scrollablePicture = new ScrollablePicture(imageIcon, 1);

		scrollablePicture.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				super.mousePressed(me);
				startX = me.getX();
				startY = me.getY();
			}
		});
		scrollablePicture.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent me) {
				super.mouseDragged(me);
				final int diffX = me.getX() - startX;
				final int diffY = me.getY() - startY;

				final JScrollBar hbar = scrollPane.getHorizontalScrollBar();
				hbar.setValue(hbar.getValue() - diffX);
				final JScrollBar vbar = scrollPane.getVerticalScrollBar();
				vbar.setValue(vbar.getValue() - diffY);
			}
		});

		return scrollablePicture;
	}

	private RenderingHints getHints() {
		final RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		return hints;
	}

	private double getZoom() {
		// if (zoomFactor <= -10) {
		// return 0.05;
		// }
		// return 1.0 + zoomFactor / 10.0;
		return Math.pow(1.1, zoomFactor);
	}

	private void copy() {
		final GeneratedImage generatedImage = simpleLine2.getGeneratedImage();
		if (generatedImage == null) {
			return;
		}
		final File png = generatedImage.getImage();
		final Image image = Toolkit.getDefaultToolkit().createImage(png.getAbsolutePath());
		final ImageSelection imgSel = new ImageSelection(image);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
	}

	public SimpleLine2 getSimpleLine() {
		return simpleLine2;
	}

	private int v1;
	private int v2;
	private JSVGCanvas svgCanvas;

	public void refreshImage(boolean external) {
		final JScrollBar bar1 = scrollPane.getVerticalScrollBar();
		final JScrollBar bar2 = scrollPane.getHorizontalScrollBar();
		if (external && isError() == false) {
			v1 = bar1.getValue();
			v2 = bar2.getValue();
		}
		scrollPane.setViewportView(buildScrollablePicture());
		force();
		if (external) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					refreshSimpleLine();
					if (isError() == false) {
						bar1.setValue(v1);
						bar2.setValue(v2);
					}
				}
			});
		}
	}

	private boolean isError() {
		return simpleLine2.getGeneratedImage() != null && simpleLine2.getGeneratedImage().lineErrorRaw() != -1;

	}

	private void force() {
		// setVisible(true);
		repaint();
		// validate();
		// getContentPane().validate();
		// getContentPane().setVisible(true);
		// getContentPane().repaint();
		// scrollPane.validate();
		// scrollPane.setVisible(true);
		// scrollPane.repaint();
	}

}

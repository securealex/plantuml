package plantuml;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;

import org.apache.batik.bridge.ViewBox;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.SVGUserAgent;
import org.w3c.dom.svg.SVGPreserveAspectRatio;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;

public class MyGraph extends JSVGCanvas {

	protected int xBorder = 8;
	protected int yBorder = 8;
	protected boolean autoFitToCanvas;
	protected boolean stopProcessingOnDispose;

	public MyGraph() {
	}

	public MyGraph(SVGUserAgent ua, boolean eventsEnabled, boolean selectableText) {
		super(ua, eventsEnabled, selectableText);
	}

	public boolean isAutoFitToCanvas() {
		return autoFitToCanvas;
	}

	public void setAutoFitToCanvas(boolean autoFit) {
		this.autoFitToCanvas = autoFit;
		if (autoFit)
			setRecenterOnResize(true);
	}

	public boolean isStopProcessingOnDispose() {
		return stopProcessingOnDispose;
	}

	public void setStopProcessingOnDispose(boolean stopProcessingOnDispose) {
		this.stopProcessingOnDispose = stopProcessingOnDispose;
	}

	/** note the border will be ignored if canvas width/height <= 4 * borderX/Y **/
	public void setInnerBorderWidth(int borderX, int borderY) {
		this.xBorder = borderX;
		this.yBorder = borderY;
	}

	@Override
	public void dispose() {
		if (stopProcessingOnDispose)
			stopProcessing();
		super.dispose();
	}

	/**
	 * overwrites calculateViewingTransform to allow autoFitToCanvas behaviour, if autoFitToCanvas is not set it will use the base's class method.
	 *
	 * Right now fragIdent != null IS NOT SUPPORTED !!!
	 */
	@Override
	protected AffineTransform calculateViewingTransform(String fragIdent, SVGSVGElement svgElt) {
		assert fragIdent == null; // don't understand this parameter, have not found a simple test case yet
		if (!autoFitToCanvas || fragIdent != null)
			return super.calculateViewingTransform(fragIdent, svgElt);
		// canvas size / additional border
		Dimension d = getSize();
		int xb = 0, yb = 0;
		if (d.width < 1)
			d.width = 1;
		if (d.height < 1)
			d.height = 1;
		if (d.width > 4 * xBorder) // if canvas is large enough add border
			d.width -= 2 * (xb = xBorder);
		if (d.height > 4 * yBorder) // if canvas is large enough add border
			d.height -= 2 * (yb = yBorder);
		//
		AffineTransform tf;
		//
		String viewBox = svgElt.getAttributeNS(null, ViewBox.SVG_VIEW_BOX_ATTRIBUTE);
		if (viewBox.length() == 0) {
			// no viewbox specified, make an own one
			float[] vb = calculateDefaultViewbox(fragIdent, svgElt);
			tf = ViewBox.getPreserveAspectRatioTransform(vb, SVGPreserveAspectRatio.SVG_PRESERVEASPECTRATIO_XMIDYMID, true, d.width, d.height);
		} else {
			String aspectRatio = svgElt.getAttributeNS(null, ViewBox.SVG_PRESERVE_ASPECT_RATIO_ATTRIBUTE);
			if (aspectRatio.length() > 0)
				tf = ViewBox.getPreserveAspectRatioTransform(svgElt, viewBox, aspectRatio, d.width, d.height, bridgeContext);
			else {
				float[] vb = ViewBox.parseViewBoxAttribute(svgElt, viewBox, bridgeContext);
				tf = ViewBox.getPreserveAspectRatioTransform(vb, SVGPreserveAspectRatio.SVG_PRESERVEASPECTRATIO_XMIDYMAX, true, d.width, d.height);
			}
		}
		if (xb > 0 || yb > 0) { // center image
			AffineTransform tf2 = AffineTransform.getTranslateInstance(xb, yb);
			tf2.concatenate(tf);
			tf = tf2;
		}
		return tf;
	}

	protected float[] calculateDefaultViewbox(String fragIdent, SVGSVGElement svgElt) {
		float[] vb = new float[4];
		SVGRect rc = svgElt.getBBox();
		vb[0] = rc.getX();
		vb[1] = rc.getY();
		vb[2] = rc.getWidth();
		vb[3] = rc.getHeight();
		return vb;
	}

}
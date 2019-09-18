package net.robinx.lib.blurview;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

import android.util.Log;

public class SVGParser {

	static final String TAG = "SVGParser";

  public static Path parsePath(String pathString, float density) {
      int n = pathString.length();
      ParserHelper ph = new ParserHelper(pathString, 0);
      ph.skipWhitespace();
      Path p = new Path();
      float lastX = 0;
      float lastY = 0;
      float lastX1 = 0;
      float lastY1 = 0;
      float subPathStartX = 0;
      float subPathStartY = 0;
      char prevCmd = 0;
      while (ph.pos < n) {
        char cmd = pathString.charAt(ph.pos);
        switch (cmd) {
        case '-':
        case '+':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          if (prevCmd == 'm' || prevCmd == 'M') {
            cmd = (char) ((prevCmd) - 1);
            break;
          } else if (("lhvcsqta").indexOf(Character.toLowerCase(prevCmd)) >= 0) {
            cmd = prevCmd;
            break;
          }
        default: {
          ph.advance();
          prevCmd = cmd;
        }
        }
  
        boolean wasCurve = false;
        switch (cmd) {
        case 'M':
        case 'm': {
          float x = ph.nextFloat() * density;
          float y = ph.nextFloat() * density;
          if (cmd == 'm') {
            subPathStartX += x;
            subPathStartY += y;
            p.rMoveTo(x, y);
            lastX += x;
            lastY += y;
          } else {
            subPathStartX = x;
            subPathStartY = y;
            p.moveTo(x, y);
            lastX = x;
            lastY = y;
          }
          break;
        }
        case 'Z':
        case 'z': {
          p.close();
          p.moveTo(subPathStartX, subPathStartY);
          lastX = subPathStartX;
          lastY = subPathStartY;
          lastX1 = subPathStartX;
          lastY1 = subPathStartY;
          wasCurve = true;
          break;
        }
        case 'T':
        case 't':
          // todo - smooth quadratic Bezier (two parameters)
          throw new UnsupportedOperationException("svg parse 'T/t' not supported");
          
        case 'L':
        case 'l': {
          float x = ph.nextFloat() * density;
          float y = ph.nextFloat() * density;
          if (cmd == 'l') {
            p.rLineTo(x, y);
            lastX += x;
            lastY += y;
          } else {
            p.lineTo(x, y);
            lastX = x;
            lastY = y;
          }
          break;
        }
        case 'H':
        case 'h': {
          float x = ph.nextFloat() * density;
          if (cmd == 'h') {
            p.rLineTo(x, 0);
            lastX += x;
          } else {
            p.lineTo(x, lastY);
            lastX = x;
          }
          break;
        }
        case 'V':
        case 'v': {
          float y = ph.nextFloat() * density;
          if (cmd == 'v') {
            p.rLineTo(0, y);
            lastY += y;
          } else {
            p.lineTo(lastX, y);
            lastY = y;
          }
          break;
        }
        case 'C':
        case 'c': {
          wasCurve = true;
          float x1 = ph.nextFloat() * density;
          float y1 = ph.nextFloat() * density;
          float x2 = ph.nextFloat() * density;
          float y2 = ph.nextFloat() * density;
          float x = ph.nextFloat() * density;
          float y = ph.nextFloat() * density;
          if (cmd == 'c') {
            x1 += lastX;
            x2 += lastX;
            x += lastX;
            y1 += lastY;
            y2 += lastY;
            y += lastY;
          }
          p.cubicTo(x1, y1, x2, y2, x, y);
          lastX1 = x2;
          lastY1 = y2;
          lastX = x;
          lastY = y;
          break;
        }
        case 'Q':
        case 'q':
          // todo - quadratic Bezier (four parameters)
          throw new UnsupportedOperationException("svg parse 'Q/q' not supported");
        case 'S':
        case 's': {
          wasCurve = true;
          float x2 = ph.nextFloat() * density;
          float y2 = ph.nextFloat() * density;
          float x = ph.nextFloat() * density;
          float y = ph.nextFloat() * density;
          if (Character.isLowerCase(cmd)) {
            x2 += lastX;
            x += lastX;
            y2 += lastY;
            y += lastY;
          }
          float x1 = 2 * lastX - lastX1;
          float y1 = 2 * lastY - lastY1;
          p.cubicTo(x1, y1, x2, y2, x, y);
          lastX1 = x2;
          lastY1 = y2;
          lastX = x;
          lastY = y;
          break;
        }
        case 'A':
        case 'a': {
          float rx = ph.nextFloat() * density;
          float ry = ph.nextFloat() * density;
          float theta = ph.nextFloat() * density;
          int largeArc = ph.nextFlag();
          int sweepArc = ph.nextFlag();
          float x = ph.nextFloat() * density;
          float y = ph.nextFloat() * density;
          if (cmd == 'a') {
            x += lastX;
            y += lastY;
          }
          drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
          lastX = x;
          lastY = y;
          break;
        }
        default:
          Log.w(TAG, "Invalid path command: " + cmd);
          ph.advance();
        }
        if (!wasCurve) {
          lastX1 = lastX;
          lastY1 = lastY;
        }
        ph.skipWhitespace();
      }
      return p;    
  }


  private static final RectF arcRectf = new RectF();
	private static final Matrix arcMatrix = new Matrix();
	private static final Matrix arcMatrix2 = new Matrix();
  
  private static void drawArc(Path p, float lastX, float lastY, float x, float y, 
      float rx, float ry, float theta, int largeArc, int sweepArc) {
    // Log.d("drawArc", "from (" + lastX + "," + lastY + ") to (" + x + ","+ y + ") r=(" + rx + "," + ry +
    // ") theta=" + theta + " flags="+ largeArc + "," + sweepArc);

    // http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes

    if (rx == 0 || ry == 0) {
      p.lineTo(x, y);
      return;
    }

    if (x == lastX && y == lastY) {
      return; // nothing to draw
    }

    rx = Math.abs(rx);
    ry = Math.abs(ry);

    final float thrad = theta * (float) Math.PI / 180;
    final float st = (float) Math.sin((double)thrad);
    final float ct = (float) Math.cos((double)thrad);

    final float xc = (lastX - x) / 2;
    final float yc = (lastY - y) / 2;
    final float x1t = ct * xc + st * yc;
    final float y1t = -st * xc + ct * yc;

    final float x1ts = x1t * x1t;
    final float y1ts = y1t * y1t;
    float rxs = rx * rx;
    float rys = ry * ry;

    float lambda = (x1ts / rxs + y1ts / rys) * 1.001f; // add 0.1% to be sure that no out of range occurs due to
                              // limited precision
    if (lambda > 1) {
      float lambdasr = (float) Math.sqrt((double)lambda);
      rx *= lambdasr;
      ry *= lambdasr;
      rxs = rx * rx;
      rys = ry * ry;
    }

    final float R =
            (float) (Math.sqrt((rxs * rys - rxs * y1ts - rys * x1ts) / (rxs * y1ts + rys * x1ts))
                        * ((largeArc == (float)sweepArc) ? -1 : 1));
    final float cxt = R * rx * y1t / ry;
    final float cyt = -R * ry * x1t / rx;
    final float cx = ct * cxt - st * cyt + (lastX + x) / 2;
    final float cy = st * cxt + ct * cyt + (lastY + y) / 2;

    final float th1 = angle(1, 0, (x1t - cxt) / rx, (y1t - cyt) / ry);
    float dth = angle((x1t - cxt) / rx, (y1t - cyt) / ry, (-x1t - cxt) / rx, (-y1t - cyt) / ry);

    if (sweepArc == 0 && dth > 0) {
      dth -= 360;
    } else if (sweepArc != 0 && dth < 0) {
      dth += 360;
    }

    // draw
    if ((theta % 360) == 0) {
      // no rotate and translate need
      arcRectf.set(cx - rx, cy - ry, cx + rx, cy + ry);
      p.arcTo(arcRectf, th1, dth);
    } else {
      // this is the hard and slow part :-)
      arcRectf.set(-rx, -ry, rx, ry);

      arcMatrix.reset();
      arcMatrix.postRotate(theta);
      arcMatrix.postTranslate(cx, cy);
      arcMatrix.invert(arcMatrix2);

      p.transform(arcMatrix2);
      p.arcTo(arcRectf, th1, dth);
      p.transform(arcMatrix);
    }
  }

  private static float angle(float x1, float y1, float x2, float y2) {
		return (float) Math.toDegrees(Math.atan2(x1, y1) - Math.atan2(x2, y2)) % 360;
	}  
}
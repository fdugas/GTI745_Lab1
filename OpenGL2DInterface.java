
import java.lang.Math;
import javax.media.opengl.GL;
import com.sun.opengl.util.GLUT;

public class OpenGL2DInterface {

	// window dimensions (measured between pixel *edges*)
	private int _window_width = 10, _window_height = 10;

	// What a client means by "height" of a font
	// depends on the client's needs and what they want to draw.
	// This allows the client to specify what they mean.
	public static final int FONT_ASCENT = 0; // from baseline to top; good for chars [A-Z0-9] (except Q)
	public static final int FONT_ASCENT_PLUS_DESCENT = 1; // includes descenders, as in chars [jgpqy]
	public static final int FONT_TOTAL_HEIGHT = 2; // ascent + descent + recommended vertical spacing


	// From the manual page for glutStrokeCharacter():
	// The Mono Roman font supported by GLUT has characters that are
	// 104.76 units wide, up to 119.05 units high, and descenders can
	// go as low as 33.33 units.
	//
	// The "G_" prefix is because it is specific to GLUT.
	// We don't use the prefix "GLUT_", because that's
	// reserved for stuff that GLUT defines itself.
	private static final float G_FONT_ASCENT = 119.05f;
	private static final float G_FONT_DESCENT = 33.33f;
	private static final float G_CHAR_WIDTH = 104.76f;
	// This is the recommended spacing,
	// it's chosen (somewhat arbitrarily) to match that in a bitmap font that is 18 pixels high,
	// and uses 1 row of pixels in every character bitmap for vertical spacing.
	private static final float G_FONT_VERTICAL_SPACE = (1.0f/17)*(G_FONT_ASCENT+G_FONT_DESCENT);



	// For all of these methods, any pixel coordinates passed
	// in should be of pixel centers.
	// The upper left most pixel has its center at the origin (0,0),
	// and x increases to the right, y increases down.
	// All dimensions should be measured between pixel edges.

	// Clients that want to use this class as the exclusive means
	// of drawing stuff can call this method (i) once to initialize stuff,
	// and (ii) every time the window is resized; to ensure the projection
	// matrix is appropriately updated.
	public void resize( GL gl, int w, int h ) {
		_window_width = w;
		_window_height = h;
		gl.glViewport( 0, 0, w, h );
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glLoadIdentity();
		gl.glOrtho( 0, w, 0, h, -1, 1 );
	}

	public int getWidth() { return _window_width; }
	public int getHeight() { return _window_height; }

	// Clients that want to, for example, use this class to draw
	// 2D graphics on top of a 3D scene, can use these methods
	// to prepare for and clean up after, respectively, drawing
	// 2D stuff.
	// The pop method ensures that the projection matrix is
	// as it was before the push.
	public void pushProjection( GL gl, int w, int h ) {
		_window_width = w;
		_window_height = h;
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho( 0, w, 0, h, -1, 1 );
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
	}
	public void popProjection( GL gl ) {
		gl.glMatrixMode( GL.GL_PROJECTION );
		gl.glPopMatrix();
		gl.glMatrixMode( GL.GL_MODELVIEW );
		gl.glPopMatrix();
	}

	// These can be used if glVertex*() must be called directly,
	// e.g.
	//    OpenGL2DInterface g;
	//    ...
	//    glVertex2f( g.convertPixelX( pixel_x ), g.convertPixelY( pixel_y ) );
	//
	public float convertPixelX( float x ) {
		return x+0.5f;
	}
	public float convertPixelY( float y ) {
		y = _window_height - y - 1;
		return y+0.5f;
	}

	public void plotPixel( GL gl, int x, int y ) {
		y = _window_height - y - 1;
		gl.glBegin( GL.GL_POINTS );
			gl.glVertex2f( x+0.5f, y+0.5f );
		gl.glEnd();
	}

	public void drawLine( GL gl, int x1, int y1, int x2, int y2 ) {
		y1 = _window_height - y1 - 1;
		y2 = _window_height - y2 - 1;
		gl.glBegin( GL.GL_LINES );
			gl.glVertex2f( x1+0.5f, y1+0.5f );
			gl.glVertex2f( x2+0.5f, y2+0.5f );
		gl.glEnd();
		gl.glBegin( GL.GL_POINTS );
			gl.glVertex2f( x2+0.5f, y2+0.5f );
		gl.glEnd();
	}

	public void drawRect( GL gl, int x, int y, int w, int h ) {
		y = _window_height - y - h;
		--w;
		--h;
		gl.glBegin( GL.GL_LINE_LOOP );
			gl.glVertex2f( x+0.5f, y+0.5f );
			gl.glVertex2f( x+w+0.5f, y+0.5f );
			gl.glVertex2f( x+w+0.5f, y+h+0.5f );
			gl.glVertex2f( x+0.5f, y+h+0.5f );
		gl.glEnd();
	}

	public void fillRect( GL gl, int x, int y, int w, int h ) {
		y = _window_height - y - h;
		gl.glRecti( x, y, x+w, y+h );
	}

	public void drawRectBetweenTwoCorners( GL gl, int x1, int y1, int x2, int y2 ) {
		int tmp;
		if ( x2 < x1 ) { /* swap */ tmp = x1; x1 = x2; x2 = tmp; }
		if ( y2 < y1 ) { /* swap */ tmp = y1; y1 = y2; y2 = tmp; }
		drawRect( gl, x1, y1, x2-x1+1, y2-y1+1 );
	}

	public void drawCircle( GL gl, int x, int y, int radius, boolean filled ) {
		y = _window_height - y - 1;
		if ( filled ) {
			gl.glBegin( GL.GL_TRIANGLE_FAN );
			gl.glVertex2f( x+0.5f, y+0.5f );
		}
		else gl.glBegin( GL.GL_LINE_LOOP );
			int numSides = (int)( 2 * Math.PI * radius + 1 );
			float deltaAngle = 2 * (float)Math.PI / numSides;

			// Note: I used to loop up to "< numSides",
			// and indeed I think this is okay for the
			// non-filled case (because of the GL_LINE_LOOP),
			// however when used for drawing a triangle fan,
			// rather than produce a complete disc,
			// we have a 1-pixel-wide sliver missing.
			// Using "<=" fixes this.
			for ( int i = 0; i <= numSides; ++i ) {
				float angle = i * deltaAngle;
				gl.glVertex2f( x+radius*(float)Math.cos(angle)+0.5f, y+radius*(float)Math.sin(angle)+0.5f );
			}
		gl.glEnd();
	}

	// Draws an arc (or a pie slice, if ``filled'' is true).
	public void drawArc(
		GL gl,
		int x, int y, int radius,
		float startAngle, // Relative to the +x axis, increasing anticlockwise
		float arcAngle, // In radians; can be negative
		boolean filled,
		boolean isArrowHeadAtStart,
		boolean isArrowHeadAtEnd,
		float arrowHeadLength
	) {
		y = _window_height - y - 1;
		if ( filled ) {
			if ( arcAngle < 0 )
				gl.glFrontFace( GL.GL_CW );
			gl.glBegin( GL.GL_TRIANGLE_FAN );
			gl.glVertex2f( x+0.5f, y+0.5f );
		}
		else gl.glBegin( GL.GL_LINE_STRIP );
			int numSides = (int)( Math.abs(arcAngle) * radius + 1 );
			float deltaAngle = arcAngle / numSides;

			for ( int i = 0; i <= numSides; ++i ) {
				float angle = startAngle + i * deltaAngle;
				gl.glVertex2f( x+radius*(float)Math.cos(angle)+0.5f, y+radius*(float)Math.sin(angle)+0.5f );
			}
		gl.glEnd();

		if ( filled && arcAngle < 0 )
			gl.glFrontFace( GL.GL_CCW );

		if ( isArrowHeadAtStart || isArrowHeadAtEnd ) {

			// An arrow head (pointing down) before rotation:
			//
			//      A     B
			//       \   /
			//        \ /
			//   ------P---- x axis
			//
			// The 3 points forming the arrow head are A, P, B.
			// To make the arrow head point up, invert the signs
			// of A and B's y coordinates.
			//
			float sqrt2 = (float)Math.sqrt(2.0f);
			float Px = radius;
			float Ax = radius - arrowHeadLength/sqrt2;
			float Bx = radius + arrowHeadLength/sqrt2;
			float Py = 0;
			float Ay = arrowHeadLength/sqrt2;
			float By = arrowHeadLength/sqrt2;

			// Draw arrow head at one end.

			if ( arcAngle < 0 ) {
				// invert the direction of the arrow
				Ay = -Ay;
				By = -By;
			}
			if ( isArrowHeadAtStart ) {
				float theta = - startAngle;
				float c = (float)Math.cos( theta );
				float s = (float)Math.sin( theta );
				float Px_prime =  c*Px + s*Py;
				float Py_prime = -s*Px + c*Py;
				float Ax_prime =  c*Ax + s*Ay;
				float Ay_prime = -s*Ax + c*Ay;
				float Bx_prime =  c*Bx + s*By;
				float By_prime = -s*Bx + c*By;
				gl.glBegin( GL.GL_LINE_STRIP );
					gl.glVertex2f( x+Ax_prime+0.5f, y+Ay_prime+0.5f );
					gl.glVertex2f( x+Px_prime+0.5f, y+Py_prime+0.5f );
					gl.glVertex2f( x+Bx_prime+0.5f, y+By_prime+0.5f );
				gl.glEnd();
			}

			// Draw arrow head at other end.

			// invert the direction of the arrow
			Ay = -Ay;
			By = -By;

			if ( isArrowHeadAtEnd ) {
				float theta = - ( startAngle + arcAngle );
				float c = (float)Math.cos( theta );
				float s = (float)Math.sin( theta );
				float Px_prime =  c*Px + s*Py;
				float Py_prime = -s*Px + c*Py;
				float Ax_prime =  c*Ax + s*Ay;
				float Ay_prime = -s*Ax + c*Ay;
				float Bx_prime =  c*Bx + s*By;
				float By_prime = -s*Bx + c*By;
				gl.glBegin( GL.GL_LINE_STRIP );
					gl.glVertex2f( x+Ax_prime+0.5f, y+Ay_prime+0.5f );
					gl.glVertex2f( x+Px_prime+0.5f, y+Py_prime+0.5f );
					gl.glVertex2f( x+Bx_prime+0.5f, y+By_prime+0.5f );
				gl.glEnd();
			}
		}
	}

	// returns the width of a string given the desired height
	public static float stringWidthInPixels(
		String s,
		float height,   // string is scaled to be this high, in pixels
		int fontHeightType
	) {
		if ( s.length() == 0 ) return 0;

		float h, w_over_h;

		switch ( fontHeightType ) {
			case FONT_ASCENT :
				h = G_FONT_ASCENT;
				break;
			case FONT_ASCENT_PLUS_DESCENT :
				h = G_FONT_ASCENT + G_FONT_DESCENT;
				break;
			case FONT_TOTAL_HEIGHT :
			default:
				h = G_FONT_ASCENT + G_FONT_DESCENT + G_FONT_VERTICAL_SPACE;
				break;
		}
		w_over_h = G_CHAR_WIDTH / h;

		return height * s.length() * w_over_h;
	}

	public void drawString(
		GL gl, GLUT glut,
		int x, int y,      // lower left corner of the string
		String s,          // the string
		float height,      // string is scaled to be this high, in pixels
		int fontHeightType
	) {
		if ( s.length() == 0 ) return;

		y = _window_height - y - 1;

		float ascent; // in pixels
		switch ( fontHeightType ) {
			case FONT_ASCENT :
				ascent = height;
				break;
			case FONT_ASCENT_PLUS_DESCENT :
				ascent = height * G_FONT_ASCENT
					/ ( G_FONT_ASCENT + G_FONT_DESCENT );
				break;
			case FONT_TOTAL_HEIGHT :
			default:
				ascent = height * G_FONT_ASCENT
					/ ( G_FONT_ASCENT + G_FONT_DESCENT + G_FONT_VERTICAL_SPACE );
				break;
		}
		float y_ = y; // just to convert y to floating-point type
		y_ = y_ + height - ascent;

		gl.glPushMatrix();
			gl.glTranslatef( x+0.5f, y_+0.5f, 0 );

			// We scale the text to make its height that desired by the caller.
			float sf = ascent / G_FONT_ASCENT; // scale factor
			gl.glScalef( sf, sf, 1 );
			for ( int j = 0; j < s.length(); ++j )
				glut.glutStrokeCharacter( GLUT.STROKE_MONO_ROMAN, s.charAt(j) );

		gl.glPopMatrix();
	}

};



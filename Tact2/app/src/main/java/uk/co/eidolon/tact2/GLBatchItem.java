package uk.co.eidolon.tact2;

import uk.co.eidolon.tact2.TextureManager.Texture;

public class GLBatchItem
{
	float xPos, yPos;
	Texture texture;
	float scale;
	float a, r, g, b;
	boolean fixScale;
	int renderPass = 0;
	boolean bShowAnimation = true;
}

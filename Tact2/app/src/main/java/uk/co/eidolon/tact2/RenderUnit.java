package uk.co.eidolon.tact2;

import java.util.ArrayList;

import Tactikon.State.Tracks;

import uk.co.eidolon.tact2.TextureManager.Texture;

public class RenderUnit
{
	Texture baseTexture;
	Texture colourTexture;
	Texture shadowTexture = null;
	float x, y;
	float scale;
	float r, g, b;
	int unitId;
	int health;
	boolean veteran;
	int kills;
	boolean render;
	boolean bHasAction = false;
	int carryDepth = 0;
	boolean bAnimating = false;
	boolean bShowAnimation = true;
	ArrayList<RenderUnit> carrying = new ArrayList<RenderUnit>();
	Tracks tracks = null;
}

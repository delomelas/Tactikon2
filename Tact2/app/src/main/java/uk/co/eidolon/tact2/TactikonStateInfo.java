package uk.co.eidolon.tact2;

import java.util.ArrayList;

import Core.IState;
import Core.IState.GameStatus;
import Core.PlayerInfo;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import Tactikon.State.TactikonState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import uk.co.eidolon.shared.database.ChatDB;
import uk.co.eidolon.shared.database.ChatDB.ChatMessage;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.utils.LogoStore;
import uk.co.eidolon.shared.views.IStateInfo;

public class TactikonStateInfo implements IStateInfo
{
	int mGameID = -1;
	long mUserID = -1;
	
	BitmapDrawable bitmap;
	
	TactikonState mState;
	
	TactikonStateInfo(int GameID, long UserID)
	{
		mGameID = GameID;
		mUserID = UserID;
	}

	@Override 
	public View InflateView(Context context)
	{
		LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = vi.inflate(R.layout.state_info, null);
        return view;
	}
	
	@Override
	public void PopulateView(Context context, View v)
	{
		IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
		
		View view = v;
		if (view == null) return;
		
		view.setTag(Integer.valueOf(mGameID));
		
		if (mState != null)
		{
			ImageView miniMap = (ImageView)view.findViewById(R.id.mini_map);
		
			bitmap = MapGraphics.GeneratePreview(mState, mUserID);
			if (bitmap == null) return;
			if (miniMap == null) return;
			miniMap.setImageDrawable(bitmap);
			miniMap.invalidate();
			
			String textInfo = "";
			String playerName = "";
			String timeLeft = "";
			
			TextView textView = (TextView)view.findViewById(R.id.textinfo);
			TextView playerNameView = (TextView)view.findViewById(R.id.playername);
			TextView timeLeftView = (TextView)view.findViewById(R.id.timeleft);

			LinearLayout timeSectionView = (LinearLayout)view.findViewById(R.id.timesection);
			timeSectionView.setVisibility(View.GONE);

			if (mState.GetGameState() == GameStatus.GameOver)
			{
				int winner = mState.GetWinner();
				if (winner == -1)
				{
					// TODO what if it was somehow a draw???
				}
				PlayerInfo winnerInfo = mState.GetPlayerInfo(winner);
				playerName = winnerInfo.name + " has won - ";
				if (mState.winCondition == WinCondition.Annihilate)
				{
					playerName = playerName + " defeated all enemies.";
				} else if (mState.winCondition == WinCondition.CaptureAllBases)
				{
					playerName = playerName + " captured all enemy bases.";
				} else if (mState.winCondition == WinCondition.CaptureHQ)
				{
					playerName = playerName + " captured all HQs.";
				}
			} else if (mState.GetGameState() == GameStatus.InGame)
			{

				PlayerInfo info = mState.GetPlayerInfo(mState.playerToPlay);
				
				long userId = appWrapper.GetUserId();
				String alias = appWrapper.GetAlias();

				// TODO: Add back local game support
				if (mState.bLocalGame == true)
				{
					playerName = "Local game";
				} else {

					timeSectionView.setVisibility(View.VISIBLE);
					if (info.name.length() > 14) {
						playerName = info.name.substring(0, 10) + "...'s turn";
					} else {
						playerName = info.name + "'s turn";
					}
				}

				if (mState.bLocalGame == false)
				{
					long currentTime = System.currentTimeMillis()/1000;
					long timeSinceUpdate = currentTime - mState.GetLastUpdateTime();
					if (timeSinceUpdate < 0) timeSinceUpdate = 0;
					if (timeSinceUpdate > mState.maxTurnSeconds) timeSinceUpdate = mState.maxTurnSeconds;
					
					long timeRemaining = mState.maxTurnSeconds - timeSinceUpdate;
					
					long minsRemaining = timeRemaining / (60);

					if (minsRemaining < 2)
					{
						timeLeft = "1 min";
					} else if (minsRemaining <60)
					{
						timeLeft = minsRemaining + " mins";
					} else 
					{
						int hours = (int)((minsRemaining / 60));
						if (hours == 1)
						{
							timeLeft = ((minsRemaining / 60)) + " hour";
						} else
						{
							timeLeft = ((minsRemaining / 60)) + " hours";
						}
					}

					// TODO: get the GameStateMessage in there (maybe next line?)
				//
				}
				
			} else if (mState.GetGameState() == GameStatus.WaitingForPlayers)
			{
				if (mState.bFriendsOnly == true)
				{
					playerName = mState.createdByAlias + "'s friends-only game. ";
				}
				// TODO: Indicate there are more players to join in the player-logos
				//textInfo = textInfo + "Waiting for more players to join.\n";
			} else if(mState.GetGameState() == GameStatus.TimeOut)
			{
				// TODO: Indicate this better
				playerName = "Game ended due to insufficient players.";
			}
			
			if (mState.GetGameState() == GameStatus.WaitingForPlayers || mState.GetGameState() == GameStatus.InGame)
			{
				if (mState.winCondition == WinCondition.Annihilate)
				{
					textInfo = textInfo + "Goal: Annihilate.\n";
				} else if (mState.winCondition == WinCondition.CaptureAllBases)
				{
					textInfo = textInfo + "Goal: Capture cities.\n";
				} else if (mState.winCondition == WinCondition.CaptureHQ)
				{
					textInfo = textInfo + "Goal: Capture HQs.\n";
				}
			}
				
			if (mState.GetGameState() == GameStatus.WaitingForPlayers)
			{
				textInfo = textInfo + "Size: " + mState.mapSize + "x" + mState.mapSize + ". ";
				if (mState.mapBalanceScore == 0)
				{
					textInfo = textInfo + "Balance: great.";
				
				} else if (mState.mapBalanceScore < 2)
				{
					textInfo = textInfo + "Balance: good.";
				} else if (mState.mapBalanceScore < 4)
				{
					textInfo = textInfo + "Balance: fair.";
				} else if (mState.mapBalanceScore < 7)
				{
					textInfo = textInfo + "Balance: poor.";
				}else 
				{
					textInfo = textInfo + "Balance: unbalanced.";
				}
				
				int timeHours = mState.maxTurnSeconds / (60 * 60);
				textInfo = textInfo + " Turn time limit: " + timeHours + "h.";
				
				textInfo = textInfo + "\n";
			}

			if (mState.gameStateMessage.length() > 1)
			{
				textInfo = textInfo + mState.gameStateMessage;
			}
			
			textView.setText(textInfo);
			playerNameView.setText(playerName);
			timeLeftView.setText(timeLeft);
			
			LinearLayout playerLogos = (LinearLayout)view.findViewById(R.id.logo_holder);
			playerLogos.removeAllViews();
			
			
			LogoStore logoStore = appWrapper.GetLogoStore();
			
			for (int i = 0; i < mState.players.size(); ++i)
			{
				int iconSize = (int) context.getResources().getDimensionPixelSize(R.dimen.small_player_icon_size);
				
				if (mState.players.get(i) == -1)
				{
					// draw an empty square for this one
					ImageView imageView = new ImageView(context.getApplicationContext());
					imageView.setBackgroundColor(Color.rgb(64,  64,  64));
					
					LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(iconSize, iconSize);
					layoutParams.setMargins(2,  0,  2,  0);
					imageView.setLayoutParams(layoutParams);
					
					// if the player is dead
					playerLogos.addView(imageView);
					
				} else
				{
					PlayerInfo info = mState.GetPlayerInfo(mState.players.get(i));
					// draw the players logo
	
					RelativeLayout holder = new RelativeLayout(context.getApplicationContext());
					
					LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(iconSize, iconSize);
					layoutParams.setMargins(2,  0,  2,  0);

					holder.setLayoutParams(layoutParams);
					ImageView imageView = new ImageView(context.getApplicationContext());
					
					layoutParams = new LinearLayout.LayoutParams(iconSize, iconSize);
					imageView.setLayoutParams(layoutParams);
					
					Drawable logoDrawable = logoStore.GetLogo(info.logo);
					//if (logoDrawable != null) logoDrawable = logoDrawable.mutate();
					
					imageView.setImageDrawable(logoDrawable);
					
					holder.addView(imageView);
					playerLogos.addView(holder);
					
					if (mState.playerToPlay == info.userId)
					{
						ImageView border = new ImageView(context.getApplicationContext());
						border.setBackgroundResource(R.drawable.selected_border);
						holder.addView(border);
					}
					
					if (mState.IsPlayerAlive(info.userId) == false)
					{
						imageView.setBackgroundColor(Color.rgb(info.r/2, info.g/2, info.b/2));
						ImageView cross = new ImageView(context.getApplicationContext());
						cross.setBackgroundResource(R.drawable.cross);
						holder.addView(cross);
					} else //setColorFilter
					{
						imageView.setBackgroundColor(Color.rgb(info.r, info.g, info.b));
					}
				}
			}

			if (mState.IsPlayerAlive((int)mUserID) && mState.playerToPlay == mUserID && mState.GetGameState() == GameStatus.InGame)
			{
				//view.setBackgroundColor(Color.rgb(50, 50, 50));
				//view.setBackgroundColor(Color.rgb(50,50,50));
				view.setBackgroundResource(R.drawable.stateinfobackgroundhighlight);
				//view.setPadding(5,  5,  5,  5);
			} else
			{
				view.setBackgroundResource(R.drawable.stateinfobackground);
				//view.setPadding(5,  5,  5,  5);
				//view.setBackgroundColor(Color.rgb(10,10,10));
			}

			// any chat messages for this game?
			//ChatDB bob;
			ArrayList<ChatMessage> messages = ChatDB.getInstance(context).GetUnreadMessagesForGameID(mGameID, 0);
			boolean bUnreadMessage = false;
			if (messages.size() > 0)
			{
				bUnreadMessage = true;
			}

			ImageView messageStar = (ImageView)view.findViewById(R.id.new_message_star);
			if (bUnreadMessage == true)
			{
				messageStar.setVisibility(View.VISIBLE);
			} else
			{
				messageStar.setVisibility(View.GONE);
			}
			
			view.requestLayout();
		}
	}

	@Override
	public int GetGameID()
	{
		return mGameID;
	}
	
	@Override
	public long GetUserID()
	{
		return mUserID;
	}

	@Override
	public void Dispose()
	{
	}

	@Override
	public void PopulateStateInfo(IState state)
	{
		mState = (TactikonState)state;
	}
	
	@Override
	public IState GetState()
	{
		return (IState)mState;
	}
}

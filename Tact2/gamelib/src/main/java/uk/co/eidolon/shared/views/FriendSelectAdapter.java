package uk.co.eidolon.shared.views;

import java.util.ArrayList;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.utils.IAppWrapper;
import Network.UserInfo;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendSelectAdapter extends ArrayAdapter<Integer>
{
	private ArrayList<Integer> mItems;
    
    Context mContext;

    public FriendSelectAdapter(Context context, int textViewResourceId, ArrayList<Integer> items)
    {
            super(context, textViewResourceId, items);
            this.mItems = items;
            mContext = context;
    }
    
    void PopulateView(int userId, Context context, View view)
    {
    	if (context == null) return;
    	
    	UserInfo info = UserProfileDB.getInstance(context).GetUserInfo(userId);
    	if (info != null)
    	{
    		IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
    		ImageView logo = (ImageView)view.findViewById(R.id.logo);
    		TextView alias = (TextView)view.findViewById(R.id.alias);
    		alias.setText(info.alias);
    		
    		Drawable logoImage = appWrapper.GetLogoStore().GetLogo(info.logo);
    		if (logoImage != null)
    		{
    			logo.setBackgroundColor(info.colour);
    			logo.setImageDrawable(logoImage);
    		}
    	}
    }
    
    
    View InflateView(Context context)
    {
    	if (context == null) return null;
    	
    	LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = vi.inflate(R.layout.friend_item, null);
        return view;
    }
    
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
    	View view = convertView;
    	int o = mItems.get(position);
    	if (view == null)
    	{
    		view = InflateView(mContext);
    		
    	}
    	PopulateView(o, mContext, view);
    	
        return view;
    }
}

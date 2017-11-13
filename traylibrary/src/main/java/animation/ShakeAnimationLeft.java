package animation;

import android.animation.ObjectAnimator;
import android.view.View;

/**
 * Created by adarsh on 11/10/2017.
 */

public class ShakeAnimationLeft extends BaseViewAnimator{

    @Override
    public void prepare(View target) {
        getAnimatorAgent().playTogether(
                ObjectAnimator.ofFloat(target, "translationX", 0,-10,0)
        );
    }
}

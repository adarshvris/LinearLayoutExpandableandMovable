package animation;

/**
 * Created by adarsh on 11/10/2017.
 */

public enum Techniques {

    Shake(ShakeAnimatorRight.class),
    ShakeLeft(ShakeAnimationLeft.class);

    private Class animatorClazz;

    private Techniques(Class clazz) {
        animatorClazz = clazz;
    }

    public BaseViewAnimator getAnimator() {
        try {
            return (BaseViewAnimator) animatorClazz.newInstance();
        } catch (Exception e) {
            throw new Error("Can not init animatorClazz instance");
        }
    }
}

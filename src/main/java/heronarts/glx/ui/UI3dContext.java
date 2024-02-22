/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.glx.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.bgfx.BGFX;

import com.google.gson.JsonObject;

import heronarts.glx.View;
import heronarts.glx.event.KeyEvent;
import heronarts.glx.event.MouseEvent;
import heronarts.glx.ui.component.UIInputBox;
import heronarts.lx.LX;
import heronarts.lx.LXSerializable;
import heronarts.lx.modulator.Click;
import heronarts.lx.modulator.DampedParameter;
import heronarts.lx.modulator.LXPeriodicModulator;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.MutableParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.utils.LXUtils;

/**
 * This is a layer that contains a 3d scene with a camera. Mouse movements
 * control the camera, and the scene can contain components.
 */
public class UI3dContext extends UIObject implements LXSerializable, UILayer, UITabFocus {

  public static final int NUM_CAMERA_POSITIONS = 6;

  public static interface MovementListener {
    public void reset();
    public void translate(float x, float y, float z);
    public void rotate(float theta, float phi);
  }

  /**
   * Mode of mouse interaction
   */
  public enum MouseMode {
    /**
     * Mouse dragging events alter the camera view
     */
    VIEW,

    /**
     * Mouse dragging events invoke object movement callbacks
     */
    OBJECT;

    @Override
    public String toString() {
      switch (this) {
      case OBJECT:
        return "Move Fixtures";
      default:
      case VIEW:
        return "Move Camera";
      }
    }
  }

  public enum ProjectionMode {
    /**
     * Perspective projection
     */
    PERSPECTIVE,

    /**
     * Orthographic projection
     */
    ORTHOGRAPHIC;

    @Override
    public String toString() {
      switch (this) {
      case ORTHOGRAPHIC:
        return "Orthographic";
      case PERSPECTIVE:
      default:
        return "Perspective";
      }
    }
  };

  /**
   * Mouse interaction mode
   */
  public EnumParameter<MouseMode> mouseMode =
    new EnumParameter<MouseMode>("Mouse Mode", MouseMode.VIEW)
    .setDescription("Mouse interaction mode");

  /**
   * Projection mode
   */
  public final EnumParameter<ProjectionMode> projection =
    new EnumParameter<ProjectionMode>("Projection", ProjectionMode.PERSPECTIVE)
    .setDescription("Projection mode");

  /**
   * Perspective of view
   */
  public final BoundedParameter perspective =
    new BoundedParameter("Perspective", 60, 15, 150)
    .setExponent(2)
    .setUnits(BoundedParameter.Units.DEGREES)
    .setDescription("Camera lens perspective in degrees");

  /**
   * Depth of perspective field, exponential factor of radius by exp(10, Depth)
   */
  public final BoundedParameter depth =
    new BoundedParameter("Depth", 2, 0, 10)
    .setDescription("Camera's depth of perspective field");

  /**
   * Whether to animate between camera positions
   */
  public final BooleanParameter animation =
    new BooleanParameter("Animation", false)
    .setDescription("Whether animation between camera positions is enabled");

  /**
   * Animation time
   */
  public final BoundedParameter animationTime =
    new BoundedParameter("Animation Time", 1000, 100, 300000)
    .setExponent(2)
    .setUnits(LXParameter.Units.MILLISECONDS)
    .setDescription("Animation duration between camera positions");

  /**
   * Max velocity used to damp changes to radius (zoom)
   */
  public final MutableParameter cameraVelocity = new MutableParameter("CVel", Float.MAX_VALUE);

  /**
   * Acceleration used to change camera radius (zoom)
   */
  public final MutableParameter cameraAcceleration = new MutableParameter("CAcl", 0);

  /**
   * Max velocity used to damp changes to rotation (theta/phi)
   */
  public final MutableParameter rotationVelocity = new MutableParameter("RVel", 16*180);

  /**
   * Acceleration used to change rotation (theta/phi)
   */
  public final MutableParameter rotationAcceleration = new MutableParameter("RAcl", 0);

  /**
   * List of movement listeners when in OBJECT mouse mode
   */
  private final List<MovementListener> movementListeners = new ArrayList<MovementListener>();

  public final void addMovementListener(MovementListener listener) {
    Objects.requireNonNull(listener, "Cannot add null UI3dContext.MovementListener");
    if (this.movementListeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate UI3dContext.MovementListener: " + listener);
    }
    this.movementListeners.add(listener);
  }

  public final void removeMovementListener(MovementListener listener) {
    if (!this.movementListeners.contains(listener)) {
      throw new IllegalStateException("Cannot remove non-registered UI3dContext.MovementListener: " + listener);
    }
    this.movementListeners.remove(listener);
  }

  public class Camera implements LXSerializable {

    private static final double RANGE_LIMIT = 100000000;

    public final BooleanParameter active =
      new BooleanParameter("Active", false)
      .setDescription("Whether this camera view is active");

    public final BoundedParameter theta =
      new BoundedParameter("Theta", 0, 360)
      .setWrappable(true)
      .setUnits(BoundedParameter.Units.DEGREES)
      .setDescription("Camera azimuth about the Y-axis");

    public final BoundedParameter phi =
      new BoundedParameter("Phi", 0, -89, 89)
      .setUnits(BoundedParameter.Units.DEGREES)
      .setDescription("Camera elevation from the XZ plane");

    public final BoundedParameter radius =
      new BoundedParameter("Radius", 120, 0, RANGE_LIMIT)
      .setDescription("Camera radius");

    public final BoundedParameter x =
      new BoundedParameter("X", 0, -RANGE_LIMIT, RANGE_LIMIT)
      .setDescription("Camera X position");

    public final BoundedParameter y =
      new BoundedParameter("Y", 0, -RANGE_LIMIT, RANGE_LIMIT)
      .setDescription("Camera Y position");

    public final BoundedParameter z =
      new BoundedParameter("Z", 0, -RANGE_LIMIT, RANGE_LIMIT)
      .setDescription("Camera Z position");

    private Camera() {}

    private void reset() {
      this.theta.reset();
      this.phi.reset();
      this.radius.reset();
      this.x.reset();
      this.y.reset();
      this.z.reset();
    }

    private void set(Camera that) {
      set(that, true);
    }

    private void set(Camera that, boolean active) {
      this.theta.setValue(that.theta.getValue());
      this.phi.setValue(that.phi.getValue());
      this.radius.setValue(that.radius.getValue());
      this.x.setValue(that.x.getValue());
      this.y.setValue(that.y.getValue());
      this.z.setValue(that.z.getValue());
      if (active) {
        this.active.setValue(true);
      }
    }

    private void lerp(Camera one, Camera two, double amt) {
      double thetaOne = one.theta.getValue();
      double thetaTwo = two.theta.getValue();
      if (Math.abs(thetaOne - thetaTwo) > 180) {
        if (thetaOne < thetaTwo) {
          thetaOne += 360;
        } else {
          thetaTwo += 360;
        }
      }
      this.theta.setValue(LXUtils.lerp(thetaOne, thetaTwo, amt) % 360.);
      this.phi.setValue(LXUtils.lerp(one.phi.getValue(), two.phi.getValue(), amt));
      this.radius.setValue(LXUtils.lerp(one.radius.getValue(), two.radius.getValue(), amt));
      this.x.setValue(LXUtils.lerp(one.x.getValue(), two.x.getValue(), amt));
      this.y.setValue(LXUtils.lerp(one.y.getValue(), two.y.getValue(), amt));
      this.z.setValue(LXUtils.lerp(one.z.getValue(), two.z.getValue(), amt));
    }

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_RADIUS = "radius";
    private static final String KEY_THETA = "theta";
    private static final String KEY_PHI = "phi";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";

    @Override
    public void save(LX lx, JsonObject object) {
      object.addProperty(KEY_ACTIVE, this.active.isOn());
      object.addProperty(KEY_RADIUS, this.radius.getValue());
      object.addProperty(KEY_THETA, this.theta.getValue());
      object.addProperty(KEY_PHI, this.phi.getValue());
      object.addProperty(KEY_X, this.x.getValue());
      object.addProperty(KEY_Y, this.y.getValue());
      object.addProperty(KEY_Z, this.z.getValue());
    }

    @Override
    public void load(LX lx, JsonObject object) {
      LXSerializable.Utils.loadBoolean(this.active, object, KEY_ACTIVE);
      LXSerializable.Utils.loadDouble(this.radius, object, KEY_RADIUS);
      LXSerializable.Utils.loadDouble(this.theta, object, KEY_THETA);
      LXSerializable.Utils.loadDouble(this.phi, object, KEY_PHI);
      LXSerializable.Utils.loadDouble(this.x, object, KEY_X);
      LXSerializable.Utils.loadDouble(this.y, object, KEY_Y);
      LXSerializable.Utils.loadDouble(this.z, object, KEY_Z);
    }
  }

  public final Camera[] cue = new Camera[NUM_CAMERA_POSITIONS];

  public final ObjectParameter<Camera> focusCamera;

  public final Camera camera = new Camera();

  private Camera prevCamera = null;
  private Camera cameraFrom = new Camera();
  private Camera cameraTo = new Camera();

  private final LXPeriodicModulator animating = new Click(this.animationTime).setLooping(false);

  public final UIInputBox.ProgressIndicator animationProgress = new UIInputBox.ProgressIndicator() {

    @Override
    public boolean hasProgress() {
      return animating.isRunning();
    }

    @Override
    public double getProgress() {
      return animating.getBasis();
    }

  };

  private final DampedParameter thetaDamped =
    new DampedParameter(this.camera.theta, this.rotationVelocity, this.rotationAcceleration)
    .setModulus(360);

  private final DampedParameter phiDamped =
    new DampedParameter(this.camera.phi, this.rotationVelocity, this.rotationAcceleration);

  private final DampedParameter radiusDamped =
    new DampedParameter(this.camera.radius, this.cameraVelocity, this.cameraAcceleration);

  private final DampedParameter xDamped = new DampedParameter(
    this.camera.x, this.cameraVelocity, this.cameraAcceleration
  );

  private final DampedParameter yDamped = new DampedParameter(
    this.camera.y, this.cameraVelocity, this.cameraAcceleration
  );

  private final DampedParameter zDamped = new DampedParameter(
    this.camera.z, this.cameraVelocity, this.cameraAcceleration
  );

  // These are derived from positionX based upon the camera mode
  private final Vector3f center = new Vector3f(0, 0, 0);
  private final Vector3f eye = new Vector3f(0, 0, 0);
  private final Vector3f centerDamped = new Vector3f(0, 0, 0);
  private final Vector3f eyeDamped = new Vector3f(0, 0, 0);
  private final Vector3f up = new Vector3f(0, 1, 0);

  // Radius bounds
  private float minRadius = 1, maxRadius = Float.MAX_VALUE;

  protected final View view;
  private float x;
  private float y;
  private float width;
  private float height;

  protected UI3dContext(UI ui, float x, float y, float w, float h) {
    setUI(ui);
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
    this.view = new View(ui.lx);
    setViewRect();

    for (int i = 0; i < this.cue.length; ++i) {
      this.cue[i] = new Camera();
    }

    this.focusCamera = new ObjectParameter<Camera>("Camera", this.cue);
    addListener(this.focusCamera, p -> {
      Camera selectCamera = this.focusCamera.getObject();
      if (!selectCamera.active.isOn()) {
        // Store state into the camera
        selectCamera.set(this.camera);
      } else {
        if (this.animation.isOn() && (selectCamera != this.prevCamera)) {
          // Trigger animation from current camera to the next
          this.cameraFrom.set(this.camera);
          this.cameraTo.set(selectCamera);
          this.animating.trigger();
        } else {
          // Immediately update all camera state
          this.animating.stop();
          this.camera.set(selectCamera);
          this.thetaDamped.setValue(this.camera.theta.getValue());
          this.phiDamped.setValue(this.camera.phi.getValue());
          this.radiusDamped.setValue(this.camera.radius.getValue());
          this.xDamped.setValue(this.camera.x.getValue());
          this.yDamped.setValue(this.camera.y.getValue());
          this.zDamped.setValue(this.camera.z.getValue());
          computeCamera(true);
        }
      }
      this.prevCamera = selectCamera;
    });

    addLoopTask(this.animating);
    addLoopTask(this.thetaDamped);
    addLoopTask(this.phiDamped);
    addLoopTask(this.radiusDamped);
    addLoopTask(this.xDamped);
    addLoopTask(this.yDamped);
    addLoopTask(this.zDamped);

    this.thetaDamped.start();
    this.radiusDamped.start();
    this.phiDamped.start();
    this.xDamped.start();
    this.yDamped.start();
    this.zDamped.start();

    computeCamera(true);

    addListener(this.camera.radius, p -> {
      double value = this.camera.radius.getValue();
      if (value < this.minRadius || value > this.maxRadius) {
        this.camera.radius.setValue(LXUtils.constrain(value, this.minRadius, this.maxRadius));
      }
    });
  }

  @Override
  public void dispose() {
    this.view.dispose();
    super.dispose();
  }

  @Override
  public float getX() {
    return this.x;
  }

  @Override
  public float getY() {
    return this.y;
  }

  @Override
  public float getWidth() {
    return this.width;
  }

  @Override
  public float getHeight() {
    return this.height;
  }

  public UI3dContext setPosition(float x, float y) {
    return setRect(x, y, this.width, this.height);
  }

  public UI3dContext setSize(float width, float height) {
    return setRect(this.x, this.y, width, height);
  }

  public UI3dContext setRect(float x, float y, float width, float height) {
    if (this.x != x || this.y != y || this.width != width || this.height != height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      setViewRect();
    }
    return this;
  }

  private void setViewRect() {
    if (this.ui.lx.isOpenGL()) {
      // NOTE(mcslee): I really have no clue what is up with the Y-values here, this was
      // arrived at by horrendous trial and error
      this.view.setRect(
        (int) (this.x * this.ui.lx.getUIZoom()),
        (int) ((this.ui.getHeight() + this.y) * this.ui.lx.getUIZoom()),
        (int) (this.width * this.ui.lx.getUIZoom()),
        (int) (this.height * this.ui.lx.getUIZoom())
      );
    } else {
      // Note that we transform our rect by UI content scaling factor
      this.view.setRect(
        (int) (this.x * this.ui.getContentScaleX()),
        (int) (this.y * this.ui.getContentScaleY()),
        (int) Math.ceil(this.width * this.ui.getContentScaleX()),
        (int) Math.ceil(this.height * this.ui.getContentScaleY())
      );
    }
  }

  /**
   * Adds a component to the layer
   *
   * @param component Component
   * @return this
   */
  public final UI3dContext addComponent(UI3dComponent component) {
    if (component.getContext() != null) {
      throw new IllegalStateException("Cannot add 3d component to multiple contexts");
    }
    if (this.mutableChildren.contains(component)) {
      throw new IllegalStateException("Cannot add 3d component twice");
    }
    component.setContext(this);
    this.mutableChildren.add(component);
    return this;
  }

  /**
   * Removes a component from the layer
   *
   * @param component Component
   * @return this
   */
  public final UI3dContext removeComponent(UI3dComponent component) {
    if (!this.mutableChildren.contains(component)) {
      throw new IllegalStateException("Cannot remove 3d component that doens't belong to context");
    }
    this.mutableChildren.remove(component);
    component.setContext(null);
    return this;
  }

  /**
   * Clears the camera stored at the given index
   *
   * @param index Camera index to clear
   * @return this
   */
  public UI3dContext clearCamera(int index) {
    this.cue[index].active.setValue(false);
    return this;
  }

  /**
   * Sets the cue position index of the camera
   *
   * @param index Camera index
   * @return this
   */
  public UI3dContext setCamera(int index) {
    if (this.focusCamera.getValuei() != index) {
      this.focusCamera.setValue(index);
    } else {
      this.focusCamera.bang();
    }
    return this;
  }

  /**
   * Set radius of the camera
   *
   * @param radius Camera radius
   * @return this
   */
  public UI3dContext setRadius(float radius) {
    this.camera.radius.setValue(radius);
    return this;
  }

  /**
   * Sets perspective angle of the camera in degrees
   *
   * @param perspective Angle in degrees
   * @return this
   */
  public UI3dContext setPerspective(float perspective) {
    this.perspective.setValue(perspective);
    return this;
  }

  /**
   * Sets the camera's maximum zoom speed
   *
   * @param cameraVelocity Max units/per second radius may change by
   * @return this
   */
  public UI3dContext setCameraVelocity(float cameraVelocity) {
    this.cameraVelocity.setValue(cameraVelocity);
    return this;
  }

  /**
   * Set's the camera's zoom acceleration, 0 is infinite
   *
   * @param cameraAcceleration Acceleration for camera
   * @return this
   */
  public UI3dContext setCameraAcceleration(float cameraAcceleration) {
    this.cameraAcceleration.setValue(cameraAcceleration);
    return this;
  }

  /**
   * Sets the camera's maximum rotation speed
   *
   * @param rotationVelocity Max radians/per second viewing angle may change by
   * @return this
   */
  public UI3dContext setRotationVelocity(float rotationVelocity) {
    this.rotationVelocity.setValue(rotationVelocity);
    return this;
  }

  /**
   * Set's the camera's rotational acceleration, 0 is infinite
   *
   * @param rotationAcceleration Acceleration of camera rotation
   * @return this
   */
  public UI3dContext setRotationAcceleration(float rotationAcceleration) {
    this.rotationAcceleration.setValue(rotationAcceleration);
    return this;
  }

  /**
   * Set the theta angle of viewing
   *
   * @param theta Angle about the y axis
   * @return this
   */
  public UI3dContext setTheta(double theta) {
    this.camera.theta.setValue(theta);
    return this;
  }

  /**
   * Set the phi angle of viewing
   *
   * @param phi Angle about the y axis
   * @return this
   */
  public UI3dContext setPhi(float phi) {
    this.camera.phi.setValue(phi);
    return this;
  }

  /**
   * Sets bounds on the radius
   *
   * @param minRadius Minimum camera radius
   * @param maxRadius Maximum camera radius
   * @return this
   */
  public UI3dContext setRadiusBounds(float minRadius, float maxRadius) {
    this.minRadius = minRadius;
    this.maxRadius = maxRadius;
    setRadius(LXUtils.constrainf(this.camera.radius.getValuef(), minRadius, maxRadius));
    return this;
  }

  /**
   * Set minimum radius
   *
   * @param minRadius Minimum camera radius
   * @return this
   */
  public UI3dContext setMinRadius(float minRadius) {
    return setRadiusBounds(minRadius, this.maxRadius);
  }

  /**
   * Set maximum radius
   *
   * @param maxRadius Maximum camera radius
   * @return this
   */
  public UI3dContext setMaxRadius(float maxRadius) {
    return setRadiusBounds(this.minRadius, maxRadius);
  }

  /**
   * Sets the center of the scene, only respected in ZOOM mode
   *
   * @param x X-coordinate
   * @param y Y-coordinate
   * @param z Z-coordinate
   * @return this
   */
  public UI3dContext setCenter(float x, float y, float z) {
    this.camera.x.setValue(this.center.x = x);
    this.camera.y.setValue(this.center.y = y);
    this.camera.z.setValue(this.center.z = z);
    return this;
  }

  /**
   * Gets the center position of the scene
   *
   * @return center of scene
   */
  public Vector3f getCenter() {
    return this.center;
  }

  /**
   * Gets the latest computed eye position
   *
   * @return eye position
   */
  public Vector3f getEye() {
    return this.eye;
  }

  private final LXParameter.MultiMonitor cameraMonitor =
    new LXParameter.MultiMonitor(
      this.radiusDamped, this.thetaDamped, this.phiDamped,
      this.xDamped, this.yDamped, this.zDamped
    );

  private void computeCamera(boolean initialize) {
    if (this.animating.isRunning() || this.animating.finished()) {
      this.camera.lerp(this.cameraFrom, this.cameraTo, this.animating.getBasis());
    }

    final float rv = this.radiusDamped.getValuef();
    final double tv = Math.toRadians(this.thetaDamped.getValue());
    final double pv = Math.toRadians(this.phiDamped.getValue());

    float sintheta = (float) Math.sin(tv);
    float costheta = (float) Math.cos(tv);
    float sinphi = (float) Math.sin(pv);
    float cosphi = (float) Math.cos(pv);

    float px = this.xDamped.getValuef();
    float py = this.yDamped.getValuef();
    float pz = this.zDamped.getValuef();

    this.centerDamped.set(px, py, pz);
    if (initialize) {
      this.center.set(this.centerDamped);
    }
    this.eyeDamped.set(
      px + rv * cosphi * sintheta,
      py + rv * sinphi,
      pz - rv * cosphi * costheta
    );
    this.eye.set(this.eyeDamped);
  }

  private boolean needsClear = false;

  public final void draw(UI ui, View view) {
    if (view != this.view) {
      throw new IllegalArgumentException("Not currently supported to draw a 3dContext into a different view");
    }

    // If the view has drawn before and is now invisible,
    // need to touch to clear it
    if (!isVisible()) {
      if (this.needsClear) {
        this.view.bind();
        BGFX.bgfx_touch(this.view.getId());
        this.needsClear = false;
      }
      return;
    }

    this.needsClear = true;

    // Set the camera view matrix
    computeCamera(false);
    this.view.setCamera(this.eyeDamped, this.centerDamped, this.up);

    // Set projection matrix
    float radiusValue = this.radiusDamped.getValuef();
    switch (this.projection.getEnum()) {
    case PERSPECTIVE:
      float depthFactor = (float) Math.pow(10, this.depth.getValue());
      this.view.setPerspective(
        (float) Math.toRadians(this.perspective.getValuef()),
        getWidth() / getHeight(),
        radiusValue / depthFactor,
        radiusValue * depthFactor
      );
      break;
    case ORTHOGRAPHIC:
      float halfRadiusWidth = radiusValue * .5f;
      float halfRadiusHeight = halfRadiusWidth * getHeight() / getWidth();
      this.view.setOrthographic(-halfRadiusWidth, halfRadiusWidth, -halfRadiusHeight, halfRadiusHeight, 0, radiusValue * 10);
      break;
    }

    // Bind the view, touch it to make sure it's cleared in case no children draw
    this.view.bind();
    BGFX.bgfx_touch(this.view.getId());

    // Check if view has changed
    if (this.cameraMonitor.changed()) {
      for (UIObject child : this.mutableChildren) {
        ((UI3dComponent) child).onCameraChanged(ui, this);
      }
    }

    // Draw all the components in the scene
    for (UIObject child : this.mutableChildren) {
      ((UI3dComponent) child).draw(ui, this.view);
    }
  }

  public Matrix4f getViewMatrix() {
    return this.view.getViewMatrix();
  }

  public Matrix4f getProjectionMatrix() {
    return this.view.getProjectionMatrix();
  }

  @Override
  protected void onMousePressed(MouseEvent mouseEvent, float mx, float my) {
    super.onMousePressed(mouseEvent, mx, my);
    if (mouseEvent.getCount() > 1) {
      focus(mouseEvent);
    }
  }

  private void updateFocusedCamera() {
    this.focusCamera.getObject().set(this.camera, false);
    this.animating.stop();
  }

  private enum MouseInteraction {
    ROTATE_VIEW,
    ROTATE_OBJECT,
    ZOOM,
    TRANSLATE_XY,
    TRANSLATE_Z,
  }

  private MouseInteraction getInteraction(MouseEvent mouseEvent) {
    switch (this.mouseMode.getEnum()) {
    case OBJECT:
      if (mouseEvent.isShiftDown()) {
        if (mouseEvent.isMetaDown() || mouseEvent.isControlDown()) {
         return MouseInteraction.ROTATE_VIEW;
        }
        return MouseInteraction.TRANSLATE_Z;
      } else if (mouseEvent.isMetaDown() || mouseEvent.isControlDown()) {
        return MouseInteraction.ROTATE_OBJECT;
      }
      return MouseInteraction.TRANSLATE_XY;

    default:
    case VIEW:
      if (mouseEvent.isShiftDown()) {
        return MouseInteraction.ZOOM;
      } else if (mouseEvent.isMetaDown() || mouseEvent.isControlDown()) {
        return MouseInteraction.TRANSLATE_XY;
      }
      return MouseInteraction.ROTATE_VIEW;
    }
  }

  @Override
  protected void onMouseDragged(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    MouseInteraction interaction = getInteraction(mouseEvent);
    switch (interaction) {
    case ROTATE_VIEW:
    case ROTATE_OBJECT:
      // NOTE: this is counter-intuitive but the rotation in the theta plane is divided relative
      // to height, as we're almost always in a non-square aspect ratio and want horizontal rotation
      // to feel consistent with vertical, in terms of same number of pixels mouse-movement should
      // yield same number of degrees rotation independent of the plane
      float rt = -dx / getHeight() * 1.5f * 180f;
      float rp = dy / getHeight() * 1.5f * 180f;
      if (interaction == MouseInteraction.ROTATE_VIEW) {
        this.camera.theta.incrementValue(rt);
        this.camera.phi.incrementValue(rp);
        updateFocusedCamera();
      } else {
        for (MovementListener listener : this.movementListeners) {
          listener.rotate(rt, rp);
        }
      }
      break;

    case ZOOM:
      this.camera.radius.incrementValue(dy * 2.f / getHeight() * this.camera.radius.getValue());
      updateFocusedCamera();
      break;

    case TRANSLATE_XY:
    case TRANSLATE_Z:
      final double thetaDampedRadians = Math.toRadians(this.thetaDamped.getValue());
      final double phiDampedRadians = Math.toRadians(this.phiDamped.getValue());

      final float tanPerspective = (float) Math.tan(.5 * Math.toRadians(this.perspective.getValue()));
      float sinTheta = (float) Math.sin(thetaDampedRadians);
      float cosTheta = (float) Math.cos(thetaDampedRadians);
      float sinPhi = (float) Math.sin(phiDampedRadians);
      float cosPhi = (float) Math.cos(phiDampedRadians);

      // NOTE: this is counter-intuitive but don't be fooled, the dcx value is intentionally
      // divided by the height, not the width, because aspect ratio is factored into perspective
      final float radiusDamped = this.radiusDamped.getValuef();

      float dcx = dx * 2.f / getHeight() * radiusDamped * tanPerspective;
      float dcy = dy * 2.f / getHeight() * radiusDamped * tanPerspective;

      float tx = 0, ty = 0, tz = 0;
      if (interaction == MouseInteraction.TRANSLATE_XY) {
        // Horizontal mouse movement goes "left and right" on screen
        // Vertical mouse movement goes "up and down" on screen
        tx = -dcx * cosTheta - dcy * sinTheta * sinPhi;
        ty = dcy * cosPhi;
        tz = -dcx * sinTheta + dcy * cosTheta * sinPhi;
      } else if (interaction == MouseInteraction.TRANSLATE_Z) {
        // Horizontal mouse movement is ignored
        // Vertical mouse movement goes "in and out" of screen
        tx = -dcy * sinTheta * cosPhi;
        ty = -dcy * sinPhi;
        tz = dcy * cosTheta * cosPhi;
      }

      if (this.mouseMode.getEnum() == MouseMode.VIEW) {
        this.camera.x.incrementValue(tx);
        this.camera.y.incrementValue(ty);
        this.camera.z.incrementValue(tz);
        updateFocusedCamera();
      } else {
        for (MovementListener listener : this.movementListeners) {
          listener.translate(tx, ty, tz);
        }
      }
      break;

    }
  }

  @Override
  protected void onMouseScroll(MouseEvent mouseEvent, float mx, float my, float dx, float dy) {
    float multiplier = mouseEvent.isShiftDown() ? 3 : 1;
    this.camera.radius.incrementValue(multiplier * -dy / getHeight() * this.camera.radius.getValue());
    updateFocusedCamera();
  }

  @Override
  protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
    float degrees = 1;
    if (keyEvent.isShiftDown()) {
      degrees *= 10.f;
    }
    if (keyCode == KeyEvent.VK_LEFT) {
      keyEvent.consume();
      this.camera.theta.incrementValue(degrees);
      updateFocusedCamera();
    } else if (keyCode == KeyEvent.VK_RIGHT) {
      keyEvent.consume();
      this.camera.theta.incrementValue(-degrees);
      updateFocusedCamera();
    } else if (keyCode == KeyEvent.VK_UP) {
      keyEvent.consume();
      this.camera.phi.incrementValue(-degrees);
      updateFocusedCamera();
    } else if (keyCode == KeyEvent.VK_DOWN) {
      keyEvent.consume();
      this.camera.phi.incrementValue(degrees);
      updateFocusedCamera();
    }
  }

  private static final String KEY_ANIMATION = "animation";
  private static final String KEY_ANIMATION_TIME = "animationTime";
  private static final String KEY_CAMERA = "camera";
  private static final String KEY_CUE = "cue";
  private static final String KEY_FOCUS = "focus";
  private static final String KEY_PROJECTION = "projection";
  private static final String KEY_PERSPECTIVE = "perspective";
  private static final String KEY_DEPTH = "depth";

  @Override
  public void save(LX lx, JsonObject object) {
    object.addProperty(KEY_ANIMATION, this.animation.isOn());
    object.addProperty(KEY_ANIMATION_TIME, this.animationTime.getValue());
    object.addProperty(KEY_PROJECTION, this.projection.getValuei());
    object.addProperty(KEY_PERSPECTIVE, this.perspective.getValue());
    object.addProperty(KEY_DEPTH, this.depth.getValue());
    object.add(KEY_CAMERA, LXSerializable.Utils.toObject(lx, this.camera));
    object.add(KEY_CUE, LXSerializable.Utils.toArray(lx, this.cue));
    object.addProperty(KEY_FOCUS, this.focusCamera.getValuei());
  }

  @Override
  public void load(LX lx, JsonObject object) {
    // Stop animation
    this.animating.stop();
    this.animation.setValue(false);

    LXSerializable.Utils.loadDouble(this.animationTime, object, KEY_ANIMATION_TIME);
    LXSerializable.Utils.loadInt(this.projection, object, KEY_PROJECTION);
    LXSerializable.Utils.loadDouble(this.perspective, object, KEY_PERSPECTIVE);
    LXSerializable.Utils.loadDouble(this.depth, object, KEY_DEPTH);
    if (object.has(KEY_CAMERA)) {
      LXSerializable.Utils.loadObject(lx, this.camera, object, KEY_CAMERA);
    } else {
      this.camera.reset();
    }
    LXSerializable.Utils.loadArray(lx, this.cue, object, KEY_CUE);
    LXSerializable.Utils.loadInt(this.focusCamera, object, KEY_FOCUS);

    // Updated damped values from loading
    this.radiusDamped.setValue(this.camera.radius.getValue());
    this.thetaDamped.setValue(this.camera.theta.getValue());
    this.phiDamped.setValue(this.camera.phi.getValue());
    this.xDamped.setValue(this.camera.x.getValue());
    this.yDamped.setValue(this.camera.y.getValue());
    this.zDamped.setValue(this.camera.z.getValue());

    // Re-initialize position
    computeCamera(true);

    // Load animation setting
    LXSerializable.Utils.loadBoolean(this.animation, object, KEY_ANIMATION);
  }

}

/**
 * Copyright 2019- Mark C. Slee, Heron Arts LLC
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

package heronarts.glx.ui.component;

import static org.lwjgl.bgfx.BGFX.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.google.gson.JsonObject;

import heronarts.glx.DynamicIndexBuffer;
import heronarts.glx.DynamicVertexBuffer;
import heronarts.glx.GLX;
import heronarts.glx.Texture;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.shader.ShaderProgram;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.glx.ui.UI3dContext;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXEngine;
import heronarts.lx.LXSerializable;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;

public class UIPointCloud extends UI3dComponent implements LXSerializable {

  private class Program extends ShaderProgram {

    private final Uniform.Sampler uniformTextureBase;
    private final Uniform.Sampler uniformTextureSparkle;
    private final Uniform.Vec4f uniformDimensions;
    private final Uniform.Vec4f uniformSparkle;
    private final Uniform.Vec4f uniformDirectional;
    private final Uniform.Vec4f uniformEyePosition;

    Program(GLX glx) {
      super(glx, "vs_led", "fs_led");
      this.uniformTextureBase = new Uniform.Sampler(glx, "s_texColor");
      this.uniformTextureSparkle = new Uniform.Sampler(glx, "s_texSparkle");
      this.uniformDimensions = new Uniform.Vec4f(glx, "u_dimensions");
      this.uniformSparkle = new Uniform.Vec4f(glx, "u_sparkle");
      this.uniformDirectional = new Uniform.Vec4f(glx, "u_directional");
      this.uniformEyePosition = new Uniform.Vec4f(glx, "u_eyePosition");
    }

    @Override
    public void dispose() {
      this.uniformTextureBase.dispose();
      this.uniformTextureSparkle.dispose();
      this.uniformDimensions.dispose();
      this.uniformSparkle.dispose();
      this.uniformDirectional.dispose();
      this.uniformEyePosition.dispose();
      super.dispose();
    }

    @Override
    public void setUniforms(View view) {
      this.uniformTextureBase.setTexture(0, textures[params.ledStyle.getValuei()], BGFX_SAMPLER_NONE);
      this.uniformTextureSparkle.setTexture(1, sparkles[params.ledStyle.getValuei()], BGFX_SAMPLER_U_BORDER | BGFX_SAMPLER_V_BORDER);

      final float pointScale = switch (getContext().projection.getEnum()) {
        case PERSPECTIVE -> 2f * params.pointSize.getValuef() / view.getAspectRatio();
        case ORTHOGRAPHIC -> 2f * params.pointSize.getValuef() / LXUtils.maxf(1f, getContext().getRadius());
      };
      this.uniformDimensions.set(
        global.contrast.getValuef(),
        params.feather.getValuef(),
        view.getAspectRatio(),
        pointScale
      );

      this.uniformSparkle.set(
        params.sparkleAmount.getValuef(),
        params.sparkleCurve.getValuef(),
        (float) Math.toRadians(params.sparkleRotate.getValue()),
        (lx.engine.nowMillis % 30000) * LX.TWO_PIf / 30000f
      );

      this.uniformDirectional.set(
        (params.directional.getEnum() == DirectionStyle.DIRECTED) ? 1f : 0f,
        (float) Math.cos(.5 * Math.toRadians(params.directionalDispersion.getValuef())),
        LXUtils.lerpf(1f, .1f, params.directionalContrast.getValuef())
      );

      final Vector3f eye = getContext().getEye();
      this.uniformEyePosition.set(eye.x, eye.y, eye.z);
    }
  }

  private class IndexBuffer extends DynamicIndexBuffer {

    private static class Point {
      private final LXPoint point;
      private float zDepth;

      private Point(LXPoint point) {
        this.point = point;
      }
    }

    private static final Comparator<Point> Z_COMPARATOR = new Comparator<Point>() {
      @Override
      public int compare(Point p1, Point p2) {
        if (p1.zDepth < p2.zDepth) {
          return 1;
        } else if (p1.zDepth > p2.zDepth) {
          return -1;
        }
        return 0;
      }
    };

    private final Point[] orderedPoints;

    private static final int INDICES_PER_POINT = 6;

    public IndexBuffer(GLX glx) {
      super(glx, model.size * INDICES_PER_POINT, true);

      this.orderedPoints = new Point[model.size];
      int i = 0;
      for (LXPoint p : model.points) {
        this.orderedPoints[i++] = new Point(p);
      }
      sortAndUpdate();
    }

    protected void sortAndUpdate() {
      // long start = System.currentTimeMillis();

      final Matrix4f viewMatrix = getContext().getViewMatrix();
      final float m02 = viewMatrix.m02();
      final float m12 = viewMatrix.m12();
      final float m22 = viewMatrix.m22();

      for (Point p : this.orderedPoints) {
        p.zDepth = m02 * p.point.x + m12 * p.point.y + m22 * p.point.z;
      }
      Arrays.sort(this.orderedPoints, Z_COMPARATOR);

      putData();
      update();

      // long end = System.currentTimeMillis();
      // GLX.log("Sorted " + this.orderedPoints.length + " points in: " + (end-start) + "ms");
    }

    protected void putData() {
      final ByteBuffer buffer = getIndexData();
      buffer.rewind();
      for (Point point : this.orderedPoints) {
        int index = point.point.index * ModelBuffer.VERTICES_PER_POINT;
        buffer.putInt(index);
        buffer.putInt(index+1);
        buffer.putInt(index+2);
        buffer.putInt(index+2);
        buffer.putInt(index+1);
        buffer.putInt(index+3);
      }
      buffer.flip();
      update();
    }
  }

  private class NormalBuffer extends VertexBuffer {

    private static final int VERTICES_PER_POINT = 2;

    private NormalBuffer(GLX lx) {
      super(lx, model.size * VERTICES_PER_POINT, VertexDeclaration.Attribute.POSITION);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      bufferDirectionalNormalLength = directionalShowNormalsLength.getValuef();
      for (LXPoint p : model.points) {
        putVertex(p.x, p.y, p.z);
        putVertex(
          p.x + bufferDirectionalNormalLength * p.xnormal,
          p.y + bufferDirectionalNormalLength * p.ynormal,
          p.z + bufferDirectionalNormalLength * p.znormal
        );
      }
    }
  }

  private class ModelBuffer extends VertexBuffer {

    private static final int VERTICES_PER_POINT = 4;

    private ModelBuffer(GLX lx) {
      super(lx, model.size * VERTICES_PER_POINT, VertexDeclaration.Attribute.POSITION, VertexDeclaration.Attribute.TEXCOORD1, VertexDeclaration.Attribute.NORMAL);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      for (LXPoint p : model.points) {
        putVertex(p.x, p.y, p.z);
        putTex3d(0f, 0f, p.size);
        putVertex(p.xnormal, p.ynormal, p.znormal);

        putVertex(p.x, p.y, p.z);
        putTex3d(1f, 0f, p.size);
        putVertex(p.xnormal, p.ynormal, p.znormal);

        putVertex(p.x, p.y, p.z);
        putTex3d(0f, 1f, p.size);
        putVertex(p.xnormal, p.ynormal, p.znormal);

        putVertex(p.x, p.y, p.z);
        putTex3d(1f, 1f, p.size);
        putVertex(p.xnormal, p.ynormal, p.znormal);
      }

    }
  }

  public enum LedStyle {

    LENS1("Lens 1", "led1.ktx", "sparkle1.ktx"),
    LENS2("Lens 2", "led2.ktx", "sparkle2.ktx"),
    LENS3("Lens 3", "led3.ktx", "sparkle3.ktx"),
    CIRCLE("Circle", "led4.ktx", "sparkle4.ktx"),
    SQUARE("Square", "led5.ktx", "sparkle4.ktx");

    public final String label;
    public final String texture;
    public final String sparkle;

    private LedStyle(String label, String texture, String sparkle) {
      this.label = label;
      this.texture = texture;
      this.sparkle = sparkle;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public enum DirectionStyle {
    OMNI("Omni"),
    DIRECTED("Directed");

    public final String label;

    private DirectionStyle(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final BoundedParameter pointSize =
    new BoundedParameter("Point Size", 3, .1, 100000)
    .setDescription("Size of points rendered in the preview display");

  public final BoundedParameter feather =
    new BoundedParameter("Feather", .5)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Percentage by which to reduce the point size as brightness is lower");

  public final BoundedParameter sparkleAmount =
    new BoundedParameter("Sparkle", 1)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Percentage of sparkle to add as the colors are brighter");

  public final BoundedParameter sparkleCurve =
    new BoundedParameter("Sparkle Curve", 2, 0, 4)
    .setDescription("Exponential curve to sparkle introduction");

  public final BoundedParameter sparkleRotate =
    new BoundedParameter("Sparkle Rotate", 45, 0, 360)
    .setUnits(BoundedParameter.Units.DEGREES)
    .setDescription("Amount sparkle rotates as it brightens");

  public final EnumParameter<DirectionStyle> directional =
    new EnumParameter<DirectionStyle>("Directional", DirectionStyle.OMNI)
    .setDescription("Whether points cast light directionally or everywhere");

  public final BoundedParameter directionalDispersion =
    new BoundedParameter("Directional Dispersion", 180, 30, 180)
    .setUnits(BoundedParameter.Units.DEGREES)
    .setDescription("Beam angle of directed lighting");

  public final BoundedParameter directionalContrast =
    new BoundedParameter("Directional Contrast", 0)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Boost contrast of directed lighting, 0% is cosine falloff");

  public final BooleanParameter directionalShowNormals =
    new BooleanParameter ("Show Direction", false)
    .setDescription("Show normal vectors for light directions");

  public final BoundedParameter directionalShowNormalsLength =
    new BoundedParameter("Direction Length", 5, 1, 10000)
    .setDescription("Length of normal vectors showing light direction");

  public final BoundedParameter contrast =
    new BoundedParameter("Contrast", 1, 1, 10)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setExponent(2)
    .setDescription("Boost contrast of UI simulation, 100% is normal, higher values artificially increase screen brightness");

  public final DiscreteParameter gammaFloor =
    new DiscreteParameter("Floor", 1, 1, 256)
    .setDescription("Gamma table floor, amount of light output for the dimmest non-zero value (1-255)");

  public final BoundedParameter gammaPow =
    new BoundedParameter ("Gamma", 1, .25, 4)
    .setDescription("Gamma table curve, shaping from dimmest to brightest (1 is linear)");

  public final DiscreteParameter alphaRef =
    new DiscreteParameter("Alpha Cutoff", 0, 256)
    .setDescription("At which alpha level to discard the point texture (0 shows everything)");

  public final BooleanParameter depthTest =
    new BooleanParameter("Depth Test", true)
    .setDescription("Whether to use depth test in rendering");

  public final EnumParameter<LedStyle> ledStyle =
    new EnumParameter<LedStyle>("LED Style", LedStyle.LENS1)
    .setDescription("Which LED texture to render");

  public final BooleanParameter useCustomParams =
    new BooleanParameter("Use Custom Params", false)
    .setDescription("Use custom parameter settings");

  private volatile boolean gammaStale = false;

  public final UIPointCloud global;
  private UIPointCloud params;

  private final GLX lx;

  private final Program program;
  private final Texture[] textures = new Texture[LedStyle.values().length];
  private final Texture[] sparkles = new Texture[LedStyle.values().length];

  private NormalBuffer normalBuffer;
  private ModelBuffer modelBuffer;
  private DynamicVertexBuffer colorBuffer;
  private IndexBuffer indexBuffer;

  // This is the model that our current vertex buffers (UI thread) is based upon,
  // which could be a frame behind the engine!
  private LXModel model = null;

  private int modelGeneration = -1;
  private float bufferDirectionalNormalLength = -1;

  private boolean auxiliary = false;

  private final LXParameter.Collection parameters = new LXParameter.Collection();

  private final int[] gammaLut;

  public UIPointCloud(GLX lx) {
    this(lx, null);
  }

  public UIPointCloud(GLX glx, UIPointCloud global) {
    this.lx = glx;
    this.program = new Program(glx);
    int ti = 0;
    for (LedStyle ledStyle : LedStyle.values()) {
      this.textures[ti] = new Texture(glx, ledStyle.texture);
      this.sparkles[ti] = new Texture(glx, ledStyle.sparkle);
      ++ti;
    };
    this.indexBuffer = null;
    this.colorBuffer = null;
    this.modelBuffer = null;
    this.global = (global != null) ? global : this;

    this.parameters.add("ledStyle", this.ledStyle);
    this.parameters.add("pointSize", this.pointSize);
    this.parameters.add("alphaRef", this.alphaRef);
    this.parameters.add("feather", this.feather);
    this.parameters.add("sparkle", this.sparkleAmount);
    this.parameters.add("sparkleCurve", this.sparkleCurve);
    this.parameters.add("sparkleRotate", this.sparkleRotate);
    this.parameters.add("directional", this.directional);
    this.parameters.add("directionalDispersion", this.directionalDispersion);
    this.parameters.add("directionalContrast", this.directionalContrast);
    this.parameters.add("directionalShowNormals", this.directionalShowNormals);
    this.parameters.add("directionalShowNormalsLength", this.directionalShowNormalsLength);
    this.parameters.add("contrast", this.contrast);
    this.parameters.add("gammaFloor", this.gammaFloor);
    this.parameters.add("gammaPow", this.gammaPow);
    this.parameters.add("depthTest", this.depthTest);
    this.parameters.add("useCustomParams", this.useCustomParams);

    addListener(this.useCustomParams, p -> {
      this.params = this.useCustomParams.isOn() ? this : this.global;
    }, true);

    // Global UIPointCloud is responsible for the gamma table
    if (this.global == this) {
      this.gammaLut = new int[256];
      this.gammaStale = true;
      addListener(this.gammaFloor, p -> this.gammaStale = true);
      addListener(this.gammaPow, p -> this.gammaStale = true);
    } else {
      this.gammaLut = null;
    }
  }

  public boolean isGlobal() {
    return this == this.global;
  }

  public UIPointCloud setAuxiliary(boolean auxiliary) {
    this.auxiliary = auxiliary;
    return this;
  }

  @Override
  public void dispose() {
    for (Texture texture : this.textures) {
      texture.dispose();
    }
    for (Texture sparkle : this.sparkles) {
      sparkle.dispose();
    }
    if (this.indexBuffer != null) {
      this.indexBuffer.dispose();
    }
    if (this.modelBuffer != null) {
      this.modelBuffer.dispose();
    }
    if (this.colorBuffer != null) {
      this.colorBuffer.dispose();
    }
    if (this.normalBuffer != null) {
      this.normalBuffer.dispose();
    }
    this.program.dispose();
    super.dispose();
  }

  private void buildModelBuffer() {
    if (this.modelBuffer != null) {
      this.modelBuffer.dispose();
    }
    this.modelBuffer = new ModelBuffer(lx);
  }

  // Need to keep the normal buffer around for at least
  // 2 frames for bgfx to not get given garbage...
  private boolean flagBuildNormalBuffer = true;

  private boolean flagNormalBufferDirty = true;

  private void buildNormalBuffer() {
    if (this.flagBuildNormalBuffer) {
      if (this.normalBuffer != null) {
        this.normalBuffer.dispose();
      }
      this.normalBuffer = new NormalBuffer(lx);
      this.flagBuildNormalBuffer = false;
      this.flagNormalBufferDirty = false;
    } else {
      this.flagBuildNormalBuffer = true;
    }
  }

  private void buildColorBuffer() {
    if (this.colorBuffer != null) {
      this.colorBuffer.dispose();
    }
    this.colorBuffer = new DynamicVertexBuffer(lx, this.model.size * ModelBuffer.VERTICES_PER_POINT, VertexDeclaration.Attribute.COLOR0);
  }

  private void buildIndexBuffer() {
    if (this.indexBuffer != null) {
      this.indexBuffer.dispose();
    }
    this.indexBuffer = new IndexBuffer(lx);
  }

  @Override
  public void onDraw(UI ui, View view) {
    LXEngine.Frame frame = this.lx.uiFrame;
    LXModel frameModel = frame.getModel();
    int frameModelGeneration = frameModel.getGeneration();

    // Empty model? Don't do anything.
    if (frameModel.size == 0) {
      return;
    }

    // Is our buffer model out of date? Rebuild it if so...
    if (this.model != frameModel) {
      LXModel oldModel = this.model;
      this.model = frameModel;
      this.modelGeneration = frameModelGeneration;
      buildModelBuffer();
      this.flagNormalBufferDirty = true;
      if ((this.colorBuffer == null) || (oldModel == null) || (oldModel.size != frameModel.size)) {
        buildColorBuffer();
      }
      if ((this.indexBuffer == null) || (oldModel == null) || (oldModel.size != frameModel.size)) {
        buildIndexBuffer();
      }
    } else if (this.modelGeneration != frameModelGeneration) {
      // Model geometry (but not size) has changed, rebuild model buffer
      buildModelBuffer();
      this.flagNormalBufferDirty = true;
      this.modelGeneration = frameModelGeneration;
      this.needsZSort = true;
      this.zSortMillis = 0;
    }

    // Sort the model buffer if the camera perspective has changed
    // We employ a timeout here to avoid needlessly resorting every single frame when
    // the camera is under active motion... instead just do one sort as long as the
    // flag has been set and a timeout has elapsed.
    if (this.needsZSort && (System.currentTimeMillis() - this.zSortMillis) > Z_SORT_TIMEOUT_MS) {
      this.indexBuffer.sortAndUpdate();
      this.needsZSort = false;
    }

    // Update the gamma table if needed
    if (this.gammaStale) {
      final int floor = this.gammaFloor.getValuei();
      final int ceil = this.gammaLut.length - 1;
      final double pow = this.gammaPow.getValue();
      this.gammaLut[0] = 0;
      for (int i = 1; i < this.gammaLut.length; ++i) {
        double lerp = (i-1.) / (this.gammaLut.length-2.);
        this.gammaLut[i] = (int) Math.round(LXUtils.lerp(floor, ceil, Math.pow(lerp, pow)));
      }
      this.gammaStale = false;
    };

    // Update the color data every frame
    final ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();
    for (int c : frame.getColors(this.auxiliary)) {
      final int a = c & LXColor.ALPHA_MASK;
      final int r = (c & LXColor.R_MASK) >> LXColor.R_SHIFT;
      final int g = (c & LXColor.G_MASK) >> LXColor.G_SHIFT;
      final int b = c & LXColor.B_MASK;
      final int gammaCorrected =
        a |
        (this.global.gammaLut[r] << LXColor.R_SHIFT) |
        (this.global.gammaLut[g] << LXColor.G_SHIFT) |
        this.global.gammaLut[b];

      for (int i = 0; i < ModelBuffer.VERTICES_PER_POINT; ++i) {
        colorData.putInt(gammaCorrected);
      }
    }
    colorData.flip();
    this.colorBuffer.update();

    final long bgfxState = 0
      | BGFX_STATE_WRITE_RGB
      | BGFX_STATE_WRITE_A
      // NOTE: very nearby pixels shouldn't clip each other, we draw UIPointCloud *last* from
      // back to front. Don't write the Z values so that "stacked" lights both render
      // BGFX_STATE_WRITE_Z |
      | BGFX_STATE_BLEND_ALPHA
      | BGFX_STATE_ALPHA_REF(this.global.alphaRef.getValuei())
      | (this.depthTest.isOn() ? BGFX_STATE_DEPTH_TEST_LESS : 0)
      ;

    // Submit our drawing program!
    this.program.submit(
      view,
      bgfxState,
      this.modelBuffer,
      this.colorBuffer,
      this.indexBuffer
    );

    if ((this.directional.getEnum() == DirectionStyle.DIRECTED) && this.directionalShowNormals.isOn()) {
      if (this.bufferDirectionalNormalLength != this.directionalShowNormalsLength.getValuef()) {
        this.flagNormalBufferDirty = true;
      }

      // Try to rebuild the normal buffer if we need to on this pass or flagged on a prev pass
      if (this.flagBuildNormalBuffer || this.flagNormalBufferDirty) {
        buildNormalBuffer();
      }

      if (this.normalBuffer != null) {
        this.lx.program.uniformFill.setFillColor(0xff00ff00);
        this.lx.program.uniformFill.submit(
          view,
          BGFX_STATE_WRITE_RGB |
          BGFX_STATE_BLEND_ALPHA |
          BGFX_STATE_DEPTH_TEST_LESS |
          BGFX_STATE_PT_LINES,
          this.normalBuffer
        );
      }
    }
  }

  private static final long Z_SORT_TIMEOUT_MS = 50;
  private boolean needsZSort = false;
  private long zSortMillis = 0;

  @Override
  protected void onCameraChanged(UI ui, UI3dContext context) {
    if (!this.needsZSort) {
      this.zSortMillis = System.currentTimeMillis();
      this.needsZSort = true;
    }
  }

  @Override
  public void save(LX lx, JsonObject object) {
    LXSerializable.Utils.saveParameters(object, this.parameters);
  }

  @Override
  public void load(LX lx, JsonObject object) {
    if (object.has(LXComponent.KEY_RESET)) {
      this.parameters.reset();
    } else {
      this.directional.reset();
      this.directionalDispersion.reset();
      this.directionalContrast.reset();
      this.directionalShowNormals.reset();
      this.directionalShowNormalsLength.reset();
      LXSerializable.Utils.loadParameters(object, this.parameters);
    }
  }

}

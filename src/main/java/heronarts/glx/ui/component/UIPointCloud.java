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
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Comparator;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

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
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;

public class UIPointCloud extends UI3dComponent implements LXSerializable {

  private class Program extends ShaderProgram {
    private short uniformTextureBase;
    private short uniformTextureSparkle;
    private short uniformDimensions;
    private short uniformSparkle;
    private final FloatBuffer dimensionsBuffer;
    private final FloatBuffer sparkleBuffer;

    Program(GLX lx) {
      super(lx, "vs_led", "fs_led");
      this.uniformTextureBase = bgfx_create_uniform("s_texColor", BGFX_UNIFORM_TYPE_SAMPLER, 1);
      this.uniformTextureSparkle = bgfx_create_uniform("s_texSparkle", BGFX_UNIFORM_TYPE_SAMPLER, 1);
      this.uniformDimensions = bgfx_create_uniform("u_dimensions", BGFX_UNIFORM_TYPE_VEC4, 1);
      this.uniformSparkle = bgfx_create_uniform("u_sparkle", BGFX_UNIFORM_TYPE_VEC4, 1);
      this.dimensionsBuffer = MemoryUtil.memAllocFloat(4);
      this.sparkleBuffer = MemoryUtil.memAllocFloat(4);
    }

    @Override
    public void setVertexBuffers(View view) {
      bgfx_set_vertex_buffer(0, modelBuffer.getHandle(), 0, modelBuffer.getNumVertices());
      bgfx_set_dynamic_vertex_buffer(1, colorBuffer.getHandle(), 0, colorBuffer.getNumVertices());
      bgfx_set_dynamic_index_buffer(indexBuffer.getHandle(), 0, indexBuffer.getNumIndices());
    }

    @Override
    public void dispose() {
      bgfx_destroy_uniform(this.uniformTextureBase);
      bgfx_destroy_uniform(this.uniformTextureSparkle);
      bgfx_destroy_uniform(this.uniformDimensions);
      bgfx_destroy_uniform(this.uniformSparkle);
      MemoryUtil.memFree(this.dimensionsBuffer);
      MemoryUtil.memFree(this.sparkleBuffer);
      super.dispose();
    }

    @Override
    public void setUniforms(View view) {
      bgfx_set_texture(0, this.uniformTextureBase, textures[params.ledStyle.getValuei()].getHandle(), BGFX_SAMPLER_NONE);
      bgfx_set_texture(1, this.uniformTextureSparkle, sparkles[params.ledStyle.getValuei()].getHandle(), BGFX_SAMPLER_U_BORDER | BGFX_SAMPLER_V_BORDER);
      this.dimensionsBuffer.put(0, global.contrast.getValuef());
      this.dimensionsBuffer.put(1, params.feather.getValuef());
      this.dimensionsBuffer.put(2, view.getAspectRatio());
      switch (getContext().projection.getEnum()) {
      case PERSPECTIVE:
        this.dimensionsBuffer.put(3, 2.0f / view.getAspectRatio() * params.pointSize.getValuef());
        break;
      case ORTHOGRAPHIC:
        this.dimensionsBuffer.put(3, 2.0f * params.pointSize.getValuef() / view.getWidth());
        break;
      }
      this.sparkleBuffer.put(0, params.sparkleAmount.getValuef());
      this.sparkleBuffer.put(1, params.sparkleCurve.getValuef());
      this.sparkleBuffer.put(2, (float) Math.toRadians(params.sparkleRotate.getValue()));
      this.sparkleBuffer.put(3, (lx.engine.nowMillis % 30000) * LX.TWO_PIf / 30000f);

      bgfx_set_uniform(this.uniformDimensions, this.dimensionsBuffer, 1);
      bgfx_set_uniform(this.uniformSparkle, this.sparkleBuffer, 1);
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

  private class ModelBuffer extends VertexBuffer {

    private static final int VERTICES_PER_POINT = 4;

    private ModelBuffer(GLX lx) {
      super(lx, model.size * VERTICES_PER_POINT, VertexDeclaration.ATTRIB_POSITION | VertexDeclaration.ATTRIB_TEXCOORD0);
    }

    @Override
    protected void bufferData(ByteBuffer buffer) {
      for (LXPoint p : model.points) {
        putVertex(p.x, p.y, p.z);
        putTex2d(0f, 0f);

        putVertex(p.x, p.y, p.z);
        putTex2d(1f, 0f);

        putVertex(p.x, p.y, p.z);
        putTex2d(0f, 1f);

        putVertex(p.x, p.y, p.z);
        putTex2d(1f, 1f);
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

  public final BoundedParameter contrast =
    new BoundedParameter("Contrast", 1, 1, 10)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setExponent(2)
    .setDescription("Boost contrast of UI simulation, 100% is normal, higher values artificially increase screen brightness");

  public final DiscreteParameter alphaRef =
    new DiscreteParameter("Alpha Cutoff", 8, 0, 256)
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

  public final UIPointCloud global;
  private UIPointCloud params;

  private final GLX lx;

  private final Program program;
  private final Texture[] textures = new Texture[LedStyle.values().length];
  private final Texture[] sparkles = new Texture[LedStyle.values().length];

  private ModelBuffer modelBuffer;
  private DynamicVertexBuffer colorBuffer;
  private IndexBuffer indexBuffer;

  // This is the model that our current vertex buffers (UI thread) is based upon,
  // which could be a frame behind the engine!
  private LXModel model = null;

  private int modelGeneration = -1;

  private boolean auxiliary = false;

  private final LXParameter.Collection parameters = new LXParameter.Collection();

  public UIPointCloud(GLX lx) {
    this(lx, null);
  }

  public UIPointCloud(GLX lx, UIPointCloud global) {
    this.lx = lx;
    this.program = new Program(lx);
    int ti = 0;
    for (LedStyle ledStyle : LedStyle.values()) {
      this.textures[ti] = new Texture(ledStyle.texture);
      this.sparkles[ti] = new Texture(ledStyle.sparkle);
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
    this.parameters.add("contrast", this.contrast);
    this.parameters.add("depthTest", this.depthTest);
    this.parameters.add("useCustomParams", this.useCustomParams);

    addListener(this.useCustomParams, p -> {
      this.params = this.useCustomParams.isOn() ? this : this.global;
    }, true);
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
    this.program.dispose();
  }

  private void buildModelBuffer() {
    if (this.modelBuffer != null) {
      this.modelBuffer.dispose();
    }
    this.modelBuffer = new ModelBuffer(lx);
  }

  private void buildColorBuffer() {
    if (this.colorBuffer != null) {
      this.colorBuffer.dispose();
    }
    this.colorBuffer = new DynamicVertexBuffer(lx, this.model.size * ModelBuffer.VERTICES_PER_POINT, VertexDeclaration.ATTRIB_COLOR0);
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
      if ((this.colorBuffer == null) || (oldModel == null) || (oldModel.size != frameModel.size)) {
        buildColorBuffer();
      }
      if ((this.indexBuffer == null) || (oldModel == null) || (oldModel.size != frameModel.size)) {
        buildIndexBuffer();
      }
    } else if (this.modelGeneration != frameModelGeneration) {
      // Model geometry (but not size) has changed, rebuild model buffer
      buildModelBuffer();
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

    // Update the color data every frame
    final ByteBuffer colorData = this.colorBuffer.getVertexData();
    colorData.rewind();
    for (int c : frame.getColors(this.auxiliary)) {
      for (int i = 0; i < ModelBuffer.VERTICES_PER_POINT; ++i) {
        colorData.putInt(c);
      }
    }
    colorData.flip();
    this.colorBuffer.update();

    // Submit our drawing program!
    this.program.submit(
      view,
      BGFX_STATE_WRITE_RGB |
      BGFX_STATE_WRITE_Z |
      BGFX_STATE_BLEND_ALPHA |
      BGFX_STATE_ALPHA_REF(this.global.alphaRef.getValuei()) |
      (this.depthTest.isOn() ? BGFX_STATE_DEPTH_TEST_LESS : 0)
    );
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
      LXSerializable.Utils.loadParameters(object, this.parameters);
    }
  }

}

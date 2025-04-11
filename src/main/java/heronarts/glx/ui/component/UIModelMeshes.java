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

import static org.lwjgl.bgfx.BGFX.bgfx_set_transform;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.lwjgl.bgfx.BGFX;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.LXEngine;
import heronarts.lx.model.LXModel;
import heronarts.lx.transform.LXVector;

public class UIModelMeshes extends UI3dComponent {

  private final GLX lx;

  private final FloatBuffer modelMatrixBuf;
  private LXModel model = null;

  private final List<Mesh> meshes = new CopyOnWriteArrayList<>();

  private class Mesh {
    private final LXModel model;
    private final LXModel.Mesh mesh;
    private final VertexBuffer vertexBuffer;

    private Mesh(LXModel model, LXModel.Mesh mesh) {
      this.model = model;
      this.mesh = mesh;
      this.vertexBuffer = new VertexBuffer(lx, mesh.vertices.size(), VertexDeclaration.ATTRIB_POSITION) {
        @Override
        protected void bufferData(ByteBuffer buffer) {
          for (LXVector p : mesh.vertices) {
            putVertex(p.x, p.y, p.z);
          }
        }
      };
    }

    private void dispose() {
      this.vertexBuffer.dispose();
    }
  }

  public UIModelMeshes(GLX lx) {
    this.lx = lx;
    this.modelMatrixBuf = MemoryUtil.memAllocFloat(16);
  }

  @Override
  public void onDraw(UI ui, View view) {
    LXEngine.Frame frame = ui.lx.uiFrame;
    LXModel frameModel = frame.getModel();

    if (this.model != frameModel) {
      this.model = frameModel;
      updateMeshes(this.model);
    }

    // Draw all the vertex buffers
    for (Mesh mesh : this.meshes) {
      bgfx_set_transform(mesh.model.transform.put(this.modelMatrixBuf, true));
      this.lx.program.uniformFill.setFillColor(mesh.mesh.color);
      this.lx.program.uniformFill.submit(
        view,
        BGFX.BGFX_STATE_WRITE_RGB |
        BGFX.BGFX_STATE_WRITE_A |
        BGFX.BGFX_STATE_WRITE_Z |
        BGFX.BGFX_STATE_BLEND_ALPHA |
        BGFX.BGFX_STATE_DEPTH_TEST_LESS,
        mesh.vertexBuffer
      );
    }
  }

  private void updateMeshes(LXModel model) {
    this.meshes.forEach(mesh -> mesh.dispose());
    this.meshes.clear();
    List<Mesh> newMeshes = new ArrayList<>();
    _addMeshes(newMeshes, model);
    if (!newMeshes.isEmpty()) {
      this.meshes.addAll(newMeshes); // addAll for COWarraylist
    }
  }

  private void _addMeshes(List<Mesh> meshes, LXModel model) {
    if (model.meshes != null) {
      for (LXModel.Mesh mesh : model.meshes) {
        meshes.add(new Mesh(model, mesh));
      }
    }
    for (LXModel child : model.children) {
      _addMeshes(meshes, child);
    }
  }

  @Override
  public void dispose() {
    this.meshes.forEach(mesh -> mesh.dispose());
    this.meshes.clear();
    MemoryUtil.memFree(this.modelMatrixBuf);
    super.dispose();
  }

}

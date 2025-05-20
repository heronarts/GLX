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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.Texture;
import heronarts.glx.VertexBuffer;
import heronarts.glx.VertexDeclaration;
import heronarts.glx.View;
import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI3dComponent;
import heronarts.lx.LXEngine;
import heronarts.lx.model.LXModel;
import heronarts.lx.transform.LXMatrix;

public class UIModelMeshes extends UI3dComponent {

  private static final boolean DEBUG_NORMAL_VECTORS = false;

  private final GLX lx;

  private final FloatBuffer modelMatrixBuf;
  private LXModel model = null;

  private final List<Mesh> meshes = new CopyOnWriteArrayList<>();

  private abstract class Mesh {

    protected final LXModel model;
    protected final LXModel.Mesh mesh;

    protected Mesh(LXModel model, LXModel.Mesh mesh) {
      this.model = model;
      this.mesh = mesh;
    }

    protected void renderVertexBuffer(UI ui, View view, VertexBuffer vertexBuffer) {
      final long bgfxState =
        BGFX.BGFX_STATE_WRITE_RGB |
        BGFX.BGFX_STATE_WRITE_A |
        BGFX.BGFX_STATE_WRITE_Z |
        BGFX.BGFX_STATE_BLEND_ALPHA |
        BGFX.BGFX_STATE_DEPTH_TEST_LESS;

      // Check for bad texture program, don't set transform matrix
      final Texture texture = getTexture();
      if ((this.mesh.type == LXModel.Mesh.Type.TEXTURE_2D) && (texture == null)) {
        return;
      }

      bgfx_set_transform(this.model.transform.put(modelMatrixBuf, LXMatrix.BufferOrder.COLUMN_MAJOR));
      switch (this.mesh.type) {
        case UNIFORM_FILL -> {
          ui.lx.program.uniformFill.setFillColor(this.mesh.color);
          ui.lx.program.uniformFill.submit(view, bgfxState, vertexBuffer);
        }
        case TEXTURE_2D -> {
          ui.lx.program.tex2d.submitPostTransform(view, bgfxState, texture, vertexBuffer);
        }
        case PHONG -> {
          ui.lx.program.phong.setEyePosition(getContext().getEye());
          ui.lx.program.phong.setLightColor(this.mesh.lightColor);
          ui.lx.program.phong.setLightDirection(this.mesh.lightDirection);
          ui.lx.program.phong.setLighting(this.mesh.lighting);
          ui.lx.program.phong.submit(view, bgfxState, vertexBuffer);
        }
      }
    }

    protected void renderNormalBuffer(UI ui, View view, VertexBuffer normalBuffer) {
      final long bgfxState =
        BGFX.BGFX_STATE_WRITE_RGB |
        BGFX.BGFX_STATE_WRITE_A |
        BGFX.BGFX_STATE_WRITE_Z |
        BGFX.BGFX_STATE_BLEND_ALPHA |
        BGFX.BGFX_STATE_DEPTH_TEST_LESS |
        BGFX.BGFX_STATE_PT_LINES;

      bgfx_set_transform(this.model.transform.put(modelMatrixBuf, LXMatrix.BufferOrder.COLUMN_MAJOR));
      ui.lx.program.uniformFill.setFillColor(0xffff0000);
      ui.lx.program.uniformFill.submit(view, bgfxState, normalBuffer);
    }

    protected Texture getTexture() { return null; }

    protected abstract void render(UI ui, View view);

    protected abstract void dispose();
  }

  private class VertexMesh extends Mesh {

    private final VertexBuffer vertexBuffer;
    private final Texture texture;

    private static Texture loadTexture(File texture) {
      if (texture != null) {
        try {
          return Texture.from2dImage(texture.getPath().toString());
        } catch (IOException iox) {
          GLX.error("Could not load texture image from: " + texture.getPath());
        }
      }
      return null;
    }

    private VertexMesh(LXModel model, LXModel.Mesh mesh) {
      super(model, mesh);

      this.texture = loadTexture(mesh.texture);
      final boolean hasColor = (mesh.type == LXModel.Mesh.Type.PHONG);
      final boolean hasNormals = (mesh.type == LXModel.Mesh.Type.PHONG);
      final boolean hasTexture = (this.texture != null);

      int vertexDecl = VertexDeclaration.ATTRIB_POSITION;
      if (hasColor) {
        vertexDecl |= VertexDeclaration.ATTRIB_COLOR0;
      }
      if (hasNormals) {
        vertexDecl |= VertexDeclaration.ATTRIB_NORMAL;
      }
      if (hasTexture) {
        vertexDecl |= VertexDeclaration.ATTRIB_TEXCOORD0;
      }

      final LXModel.Mesh.Vertex[] normals;
      if (hasNormals) {
        normals = new LXModel.Mesh.Vertex[mesh.vertices.size() / 3];
        for (int i = 0; i < normals.length; ++i) {
          LXModel.Mesh.Vertex norm = LXModel.Mesh.Vertex.normal(
            // Note order: assuming CCW face, left-handed normal!
            mesh.vertices.get(3*i),
            mesh.vertices.get(3*i+2),
            mesh.vertices.get(3*i+1)
          );
          normals[i] = norm;
        }
      } else {
        normals = null;
      }

      this.vertexBuffer = new VertexBuffer(lx, mesh.vertices.size(), vertexDecl) {
        @Override
        protected void bufferData(ByteBuffer buffer) {
          int vIndex = 0;
          for (LXModel.Mesh.Vertex v : mesh.vertices) {
            putVertex(v.x, v.y, v.z);
            if (hasColor) {
              buffer.putInt(0xffffffff);
            }
            if (hasNormals) {
              putVertex(normals[vIndex/3].x, normals[vIndex/3].y, normals[vIndex/3].z);
            }
            if (hasTexture) {
              buffer.putFloat(v.u);
              buffer.putFloat(v.v);
            }
            ++vIndex;
          }
        }
      };

    }

    @Override
    protected Texture getTexture() {
      return this.texture;
    }

    @Override
    protected void render(UI ui, View view) {
      renderVertexBuffer(ui, view, this.vertexBuffer);
    }

    @Override
    protected void dispose() {
      this.vertexBuffer.dispose();
      if (this.texture != null) {
        this.texture.dispose();
      }
    }
  }

  private final Map<String, AssimpVBO> assimpVBOCache = new HashMap<>();

  private class AssimpVBO {

    private final List<VertexBuffer> vertexBuffers = new ArrayList<>();
    private final List<VertexBuffer> normalBuffers = new ArrayList<>();

    private int refCount;

    private AssimpVBO(String path) {
      this(path, false);
    }

    private AssimpVBO(String path, boolean invertNormals) {
      GLX.log("Assimp importing mesh: " + path);
      final AIScene aiScene = Assimp.aiImportFile(path,
        Assimp.aiProcess_Triangulate |
        Assimp.aiProcess_ConvertToLeftHanded |
        Assimp.aiProcess_GenSmoothNormals |
        0
      );
      if (aiScene == null) {
        GLX.error("Assimp.aiImportFile returned null: " + path);
        return;
      }

      try {
        final int numMeshes = aiScene.mNumMeshes();

        final PointerBuffer aiMeshes = aiScene.mMeshes();
        for (int i = 0; i < numMeshes; ++i) {
          final AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));

          final int numVertices = aiMesh.mNumVertices();
          final AIVector3D.Buffer aiVertices = aiMesh.mVertices();
          float[] vertices = new float[3 * numVertices];
          int v = 0;
          while (aiVertices.remaining() > 0) {
            final AIVector3D aiVertex = aiVertices.get();
            vertices[v++] = aiVertex.x();
            vertices[v++] = aiVertex.y();
            vertices[v++] = aiVertex.z();
          }

          final int numNormals = aiMesh.mNumVertices();
          final AIVector3D.Buffer aiNormals = aiMesh.mNormals();
          float[] normals = new float[3 * numNormals];
          // Flag to flip normals if asset file CW/CCW was inverted
          float normalSign = invertNormals ? 1 : -1;
          int n = 0;
          while (aiNormals.remaining() > 0) {
            final AIVector3D aiNormal = aiNormals.get();
            normals[n++] = normalSign * aiNormal.x();
            normals[n++] = normalSign * aiNormal.y();
            normals[n++] = normalSign * aiNormal.z();
          }

          final int numFaces = aiMesh.mNumFaces();
          GLX.debug("Mesh[" + i + "] num faces: " + numFaces);

          final List<Integer> indices = new ArrayList<>();
          final AIFace.Buffer aiFaces = aiMesh.mFaces();
          while (aiFaces.remaining() > 0) {
            final AIFace aiFace = aiFaces.get();
            final int numIndices = aiFace.mNumIndices();
            final IntBuffer aiIndices = aiFace.mIndices();
            for (int j = 0; j < numIndices; ++j) {
              indices.add(aiIndices.get(j));
            }
          }

          // Smash all the faces down into a simple stream of triangles. Could be more efficient
          // using index buffers and a shared vertex buffer, but assumption is we are not loading
          // insanely massive video game style models in the LX environment...
          this.vertexBuffers.add(new VertexBuffer(lx, indices.size(), VertexDeclaration.Attribute.POSITION, VertexDeclaration.Attribute.COLOR0, VertexDeclaration.Attribute.NORMAL) {
            @Override
            protected void bufferData(ByteBuffer buffer) {
              for (int index : indices) {
                putVertex(
                  vertices[3*index],
                  vertices[3*index + 1],
                  vertices[3*index + 2]
                );
                buffer.putInt(0xffffffff);
                putVertex(
                  normals[3*index],
                  normals[3*index + 1],
                  normals[3*index + 2]
                );
              }
            }
          });

          if (DEBUG_NORMAL_VECTORS) {
            // Visualize if there's some ish up with the loaded normals...
            this.normalBuffers.add(new VertexBuffer(lx, indices.size() * 2, VertexDeclaration.Attribute.POSITION) {
              @Override
              protected void bufferData(ByteBuffer buffer) {
                for (int index : indices) {
                  putVertex(
                    vertices[3*index],
                    vertices[3*index + 1],
                    vertices[3*index + 2]
                  );
                  putVertex(
                    vertices[3*index] + normals[3*index],
                    vertices[3*index + 1] + normals[3*index + 1],
                    vertices[3*index + 2] + normals[3*index + 2]
                  );
                }
              }
            });
          }
        }
      } catch (Throwable x) {
        GLX.error(x, "Error in Assimp mesh import: " + path);
      } finally {
        Assimp.aiReleaseImport(aiScene);
      }
      this.refCount = 1;
    }

    private void dispose() {
      this.vertexBuffers.forEach(vertexBuffer -> vertexBuffer.dispose());
      this.vertexBuffers.clear();
      this.normalBuffers.forEach(vertexBuffer -> vertexBuffer.dispose());
      this.normalBuffers.clear();
    }
  }

  private class AssimpMesh extends Mesh {

    private final String path;
    private final AssimpVBO vbo;

    private final String key;

    private AssimpMesh(LXModel model, LXModel.Mesh mesh) {
      super(model, mesh);
      this.path = mesh.file.getAbsolutePath();
      this.key = String.format("%d/%s", mesh.invertNormals ? 1 : 0, this.path);
      if (assimpVBOCache.containsKey(this.key)) {
        this.vbo = assimpVBOCache.get(this.key);
        ++this.vbo.refCount;
      } else {
        assimpVBOCache.put(this.key, this.vbo = new AssimpVBO(this.path, mesh.invertNormals));
      }
    }

    @Override
    protected void render(UI ui, View view) {
      this.vbo.vertexBuffers.forEach(vertexBuffer -> renderVertexBuffer(ui, view, vertexBuffer));
      if (DEBUG_NORMAL_VECTORS) {
        this.vbo.normalBuffers.forEach(vertexBuffer -> renderNormalBuffer(ui, view, vertexBuffer));
      }
    }

    @Override
    protected void dispose() {
      if (--this.vbo.refCount <= 0) {
        assimpVBOCache.remove(this.key);
        this.vbo.dispose();
      }
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
      mesh.render(ui, view);
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
        if (mesh.vertices != null) {
          meshes.add(new VertexMesh(model, mesh));
        } else if (mesh.file != null) {
          meshes.add(new AssimpMesh(model, mesh));
        } else {
          GLX.warning("Unknown mesh type, missing vertices and file: " + mesh);
        }
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

package me.lofienjoyer.nublada.engine.graphics.mesh;

import me.lofienjoyer.nublada.Nublada;

public class Mesh {

    private final int vaoId;
    private int vertexCount;

    public Mesh(float[] positions, int[] indices, float[] uvs, float[] normals) {
        this(Nublada.LOADER.loadToVAO(positions, indices, uvs, normals), indices.length);
    }

    public Mesh(float[] positions, int[] indices, float[] uvs) {
        this(Nublada.LOADER.loadToVAO(positions, indices, uvs), indices.length);
    }

    public Mesh(float[] positions, int[] indices) {
        this(Nublada.LOADER.loadToVAO(positions, indices), indices.length);
    }

    public Mesh(int[] positions, int[] indices) {
        this(Nublada.LOADER.loadToVAO(positions, indices), indices.length);
    }

    public Mesh(int[] positions, int[] indices, int size) {
        this(Nublada.LOADER.loadToVAO(positions, indices, size), indices.length);
    }

    public Mesh(float[] positions) {
        this(Nublada.LOADER.loadToVAO(positions), positions.length / 3);
    }

    public Mesh(int vaoId, int vertexCount) {
        this.vaoId = vaoId;
        this.vertexCount = vertexCount;
    }

    public void updateMesh(int[] positions, int[] indices, int size) {
        Nublada.LOADER.updateVAO(vaoId, positions, indices, size);
        this.vertexCount = indices.length;
    }

    public void updateMesh(int[] positions, int[] indices) {
        Nublada.LOADER.updateVAO(vaoId, positions, indices);
        this.vertexCount = indices.length;
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }

}

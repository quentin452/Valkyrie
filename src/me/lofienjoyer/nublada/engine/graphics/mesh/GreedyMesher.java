package me.lofienjoyer.nublada.engine.graphics.mesh;

import me.lofienjoyer.nublada.Nublada;
import me.lofienjoyer.nublada.engine.world.Block;
import me.lofienjoyer.nublada.engine.world.BlockRegistry;
import me.lofienjoyer.nublada.engine.world.Chunk;
import me.lofienjoyer.nublada.engine.world.ChunkPreMeshData;

import java.util.ArrayList;
import java.util.List;

import static me.lofienjoyer.nublada.engine.world.World.CHUNK_WIDTH;
import static me.lofienjoyer.nublada.engine.world.World.CHUNK_HEIGHT;

/**
 * Based on roboleary's algorithm, but improved to support non-cubic chunks
 * @see <a href="https://github.com/roboleary/GreedyMesh">GreedyMesh</a>
 */
public class GreedyMesher implements Mesher {

    private final int[] dims;

    private ChunkPreMeshData chunkData;

    private int passes = 0;

    private static final int SOUTH      = 0;
    private static final int NORTH      = 1;
    private static final int EAST       = 2;
    private static final int WEST       = 3;
    private static final int TOP        = 4;
    private static final int BOTTOM     = 5;

    private List<Integer> positions;
    private List<Integer> indices;

    private int[] positionsArray;
    private int[] indicesArray;

    public GreedyMesher(ChunkPreMeshData chunkPreMeshData) {
        this.chunkData = chunkPreMeshData;

        this.dims = new int[] { CHUNK_WIDTH, CHUNK_HEIGHT, CHUNK_WIDTH };
    }

    @Override
    public Mesher compute() {
        this.positions = new ArrayList<>(10000);
        this.indices = new ArrayList<>(6000);

        computeMesh();

        // IMPORTANT - DO NOT DELETE
        // De-references ChunkPreMeshData to avoid memory leaks
        this.chunkData = null;

        positionsArray = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            positionsArray[i] = positions.get(i);
        }
        positions = null;

        indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }
        indices = null;

        return this;
    }

    @Override
    public Mesh loadToGpu() {
        Mesh mesh = new Mesh(positionsArray, indicesArray, 2);
        positionsArray = null;
        indicesArray = null;
        return mesh;
    }

    private void computeMesh() {
        int i, j, k, l, w, h, u, v, n;

        final int[] x = new int []{0,0,0};
        final int[] q = new int []{0,0,0};
        final int[] du = new int[]{0,0,0};
        final int[] dv = new int[]{0,0,0};

        int[] mask;

        int voxelFace, voxelFace1;

        for (boolean backFace = true, b = false; b != backFace; backFace = false, b = !b) {

            for(int d = 0; d < 3; d++) {

                u = (d + 1) % 3;
                v = (d + 2) % 3;

                x[0] = 0;
                x[1] = 0;
                x[2] = 0;

                q[0] = 0;
                q[1] = 0;
                q[2] = 0;
                q[d] = 1;

                mask = new int [(dims[u] + 1) * (dims[v] + 1)];

                for(x[d] = -1; x[d] < dims[d];) {

                    n = 0;

                    for(x[v] = 0; x[v] < dims[v]; x[v]++) {

                        for(x[u] = 0; x[u] < dims[u]; x[u]++) {

                            voxelFace  = chunkData.getBlock(x[0], x[1], x[2]);
                            voxelFace1 = chunkData.getBlock((x[0] + q[0]), (x[1] + q[1]), (x[2] + q[2]));

                            if (voxelFace != 0 && BlockRegistry.getBLock(voxelFace).isTransparent()) {
                                voxelFace = 0;
                            }

                            if (voxelFace1 != 0 && BlockRegistry.getBLock(voxelFace1).isTransparent()) {
                                voxelFace1 = 0;
                            }

                            mask[n++] = ((voxelFace == 0 || voxelFace1 == 0))
                                    ? backFace ? voxelFace1 : voxelFace
                                    : 0;
                        }
                    }

                    x[d]++;

                    n = 0;

                    for(j = 0; j < dims[v]; j++) {

                        for(i = 0; i < dims[u];) {

                            if(mask[n] != 0) {

                                for(w = 1; i + w < dims[u] && mask[n + w] != 0 && mask[n + w] == mask[n]; w++) {}

                                boolean done = false;

                                for(h = 1; j + h < dims[v]; h++) {

                                    for(k = 0; k < w; k++) {

                                        if(mask[n + k + h * dims[u]] == 0 || mask[n + k + h * dims[u]] != mask[n]) { done = true; break; }
                                    }

                                    if(done) { break; }
                                }

                                if (mask[n] != 0) {
                                    x[u] = i;
                                    x[v] = j;

                                    du[0] = 0;
                                    du[1] = 0;
                                    du[2] = 0;
                                    du[u] = w;

                                    dv[0] = 0;
                                    dv[1] = 0;
                                    dv[2] = 0;
                                    dv[v] = h;

                                    Block block = BlockRegistry.getBLock(mask[n]);

                                    int [] indexes = backFace ? indexes1 : indexes2;

                                    int[] vertices = new int[8];

                                    for (int index : indexes) {
                                        indices.add(index + passes * 4);
                                    }

                                    vertices[0] = getCompressedData(x[0], x[1], x[2]);

                                    vertices[2] = getCompressedData(x[0] + dv[0], x[1] + dv[1], x[2] + dv[2]);

                                    vertices[4] = getCompressedData(x[0] + du[0], x[1] + du[1], x[2] + du[2]);

                                    vertices[6] = getCompressedData(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2]);

                                    // Texture re-orientation based on the direction
                                    if (d == 2) {
                                        if (!backFace) {
                                            // 2
                                            vertices[1] = getCompressedData(0, h, block.getNorthTexture());

                                            // 0
                                            vertices[3] = getCompressedData(0, 0, block.getNorthTexture());

                                            // 3
                                            vertices[5] = getCompressedData(w, h, block.getNorthTexture());

                                            // 1
                                            vertices[7] = getCompressedData(w, 0, block.getNorthTexture());
                                        } else {
                                            // 3
                                            vertices[1] = getCompressedData(w, h, block.getSouthTexture());

                                            // 1
                                            vertices[3] = getCompressedData(w, 0, block.getSouthTexture());

                                            // 2
                                            vertices[5] = getCompressedData(0, h, block.getSouthTexture());

                                            // 0
                                            vertices[7] = getCompressedData(0, 0, block.getSouthTexture());
                                        }
                                    } else if (d == 0) {
                                        if (backFace) {
                                            // 2
                                            vertices[1] = getCompressedData(0, w, block.getWestTexture());

                                            // 3
                                            vertices[3] = getCompressedData(h, w, block.getWestTexture());

                                            // 0
                                            vertices[5] = getCompressedData(0, 0, block.getWestTexture());

                                            // 1
                                            vertices[7] = getCompressedData(h, 0, block.getWestTexture());
                                        } else {
                                            // 2
                                            vertices[1] = getCompressedData(h, w, block.getEastTexture());

                                            // 3
                                            vertices[3] = getCompressedData(0, w, block.getEastTexture());

                                            // 1
                                            vertices[5] = getCompressedData(h, 0, block.getEastTexture());

                                            // 0
                                            vertices[7] = getCompressedData(0, 0, block.getEastTexture());
                                        }
                                    } else {
                                        if (!backFace) {
                                            // 0
                                            vertices[1] = getCompressedData(0, 0, block.getTopTexture());

                                            // 1
                                            vertices[3] = getCompressedData(h, 0, block.getTopTexture());

                                            // 2
                                            vertices[5] = getCompressedData(0, w, block.getTopTexture());

                                            // 3
                                            vertices[7] = getCompressedData(h, w, block.getTopTexture());
                                        } else {
                                            // 1
                                            vertices[1] = getCompressedData(h, 0, block.getBottomTexture());

                                            // 0
                                            vertices[3] = getCompressedData(0, 0, block.getBottomTexture());

                                            // 3
                                            vertices[5] = getCompressedData(h, w, block.getBottomTexture());

                                            // 2
                                            vertices[7] = getCompressedData(0, w, block.getBottomTexture());
                                        }
                                    }

                                    positions.add(vertices[0]);
                                    positions.add(vertices[1]);
                                    positions.add(vertices[2]);
                                    positions.add(vertices[3]);
                                    positions.add(vertices[4]);
                                    positions.add(vertices[5]);
                                    positions.add(vertices[6]);
                                    positions.add(vertices[7]);
                                    passes++;

//                                    quad(x[0],                 x[1],                   x[2],
//                                            x[0] + du[0],         x[1] + du[1],           x[2] + du[2],
//                                            x[0] + du[0] + dv[0], x[1] + du[1] + dv[1],   x[2] + du[2] + dv[2],
//                                            x[0] + dv[0],         x[1] + dv[1],           x[2] + dv[2],
//                                            w,
//                                            h,
//                                            BlockRegistry.getBLock(mask[n]),
//                                            backFace,
//                                            d);
                                }

                                for(l = 0; l < h; ++l) {

                                    for(k = 0; k < w; ++k) { mask[n + k + l * dims[u]] = 0; }
                                }

                                i += w;
                                n += w;

                            } else {

                                i++;
                                n++;
                            }
                        }
                    }
                }
            }
        }
    }

    private static final int[] indexes1 = new int[] { 2,0,1, 1,3,2 };
    private static final int[] indexes2 = new int[] { 2,3,1, 1,0,2 };

    private int getCompressedData(int x, int y, int z) {
        return z | x << 9 | y << 18;
    }

}
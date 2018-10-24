package io;

import model.Tuple;
import util.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Table Reader, implements the tuple reader and store the buffer
 * Read the table from disk and fetch a tuple
 */
public class BinaryTupleReader implements TupleReader {
    private File file;
    private RandomAccessFile readerPointer;
    private ByteBuffer bufferPage;
    private int tupleSize;
    private int tupleCount;
    private int tuplePointer;

    private long pageIndex = 0;
    private long recordTupleIndex;
    
    /*
    private long recordPosition;
    private int recordTuplePointer;
    private int recordTupleCount;
    private int recordTupleSize;
    private ByteBuffer recordBufferPage;
    */

    public BinaryTupleReader(String file) {
        this.file = new File(file);
        reset();
    }

    @Override
    public void recordPosition() {
        int maxTupleCountPerPage = (Constants.PAGE_SIZE - 2 * Constants.INT_SIZE) / (tupleSize * Constants.INT_SIZE);
        recordTupleIndex = (pageIndex - 1) * maxTupleCountPerPage + 
                        (tuplePointer - 2 * Constants.INT_SIZE) / (Constants.INT_SIZE * tupleSize);
        
        /*
        this.recordBufferPage = cloneByteBuffer(bufferPage);
        this.recordTupleSize = tupleSize;
        this.recordTupleCount = tupleCount;
        this.recordTuplePointer = tuplePointer;
        try {
            this.recordPosition = readerPointer.getFilePointer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        
    }

    @Override
    public void revertToPosition() {
        reset(recordTupleIndex);
        /*
        try {
            this.readerPointer = new RandomAccessFile(this.file, "r");
        } catch (FileNotFoundException e) {
            System.out.printf("Cannot find file %s!\n", this.file.getName());
            e.printStackTrace();
        }
        this.bufferPage = cloneByteBuffer(recordBufferPage);
        this.tupleSize = recordTupleSize;
        this.tupleCount = recordTupleCount;
        this.tuplePointer = recordTuplePointer;
        try {
            //System.out.println("succeed: " + recordPosition);
            this.readerPointer.seek(recordPosition);
        } catch (Exception e) {
            System.out.println("record position fails: " + recordPosition);
            e.printStackTrace();
        }
        */
        
    }

    private static ByteBuffer cloneByteBuffer(final ByteBuffer original) {
        // Create clone with same capacity as original.
        final ByteBuffer clone = (original.isDirect()) ?
                ByteBuffer.allocateDirect(original.capacity()) :
                ByteBuffer.allocate(original.capacity());

        // Create a read-only copy of the original.
        // This allows reading from the original without modifying it.
        final ByteBuffer readOnlyCopy = original.asReadOnlyBuffer();

        // Flip and read from the original.
        readOnlyCopy.flip();
        clone.put(readOnlyCopy);

        return clone;
    }

    @Override
    public void reset() {
        try {
            this.readerPointer = new RandomAccessFile(this.file, "r");
        } catch (FileNotFoundException e) {
            System.out.printf("Cannot find file %s!\n", this.file.getName());
        }
        readPage();
    }

    public void readPage() {
        try {
            pageIndex ++;
            this.bufferPage = ByteBuffer.allocate(Constants.PAGE_SIZE);
            FileChannel inChannel = readerPointer.getChannel();
            
            int byteRead = inChannel.read(bufferPage);
            if (byteRead == -1) {
                this.tupleCount = 0;
                this.bufferPage = null;
            }
            if (bufferPage != null) {
                tupleSize = bufferPage.getInt(0);
                tupleCount = bufferPage.getInt(Constants.INT_SIZE);
                tuplePointer = 2 * Constants.INT_SIZE;
            }
        } catch (IOException e) {
            System.out.printf("Unexpected table file format\n");
        }
    }

    @Override
    public Tuple readNextTuple() {
        Tuple tuple = null;
        if (this.tupleCount <= 0) {
            return null;
        }
        int[] tupleData = new int[tupleSize];
        if (tuplePointer >= (2 + tupleCount * tupleSize) * Constants.INT_SIZE) {
            readPage();
            if (bufferPage == null) {
                close();
                return null;
            }
        }
        for (int i = 0; i < tupleSize; ++i) {
            tupleData[i] = bufferPage.getInt(tuplePointer);
            tuplePointer += Constants.INT_SIZE;
        }
        tuple = new Tuple(tupleData);
        return tuple;
    }

    @Override
    public void moveBack() {
        if (tuplePointer > 2 * Constants.INT_SIZE) {
            tuplePointer -= tupleSize * Constants.INT_SIZE;
        }
    }

    // the next tuple to read is the ith tuple
    @Override
    public void reset(long i){
        int maxTupleCountPerPage = (Constants.PAGE_SIZE - 2 * Constants.INT_SIZE) / (tupleSize * Constants.INT_SIZE);
        long pageIndex = i / maxTupleCountPerPage;
        long newTuplePointer = ((i % maxTupleCountPerPage) * tupleSize + 2) * Constants.INT_SIZE;
        long newReaderPointer = (long) pageIndex * Constants.PAGE_SIZE;
        if (this.pageIndex - 1 != pageIndex || tupleCount == 0) {
            //need to reread bufferpage
            try {
                this.readerPointer = new RandomAccessFile(this.file, "r");
                readerPointer.seek(newReaderPointer);
                this.pageIndex = pageIndex;
                readPage();
            } catch (Exception e){
                System.err.println("Failed to reset tuple");
                e.printStackTrace();
            }
        }
        tuplePointer = (int)newTuplePointer;
    }

    @Override
    public void close(){
        try {
            readerPointer.getChannel().close();
            readerPointer.close();
        } catch (IOException e) {
            System.out.print(e.getMessage());
        }
    }
}
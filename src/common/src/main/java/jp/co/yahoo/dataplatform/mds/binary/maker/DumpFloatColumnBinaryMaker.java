/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.yahoo.dataplatform.mds.binary.maker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import java.util.List;
import java.util.ArrayList;

import jp.co.yahoo.dataplatform.mds.compressor.FindCompressor;
import jp.co.yahoo.dataplatform.mds.compressor.ICompressor;
import jp.co.yahoo.dataplatform.mds.constants.PrimitiveByteLength;
import jp.co.yahoo.dataplatform.mds.spread.column.ICell;
import jp.co.yahoo.dataplatform.mds.spread.column.PrimitiveCell;
import jp.co.yahoo.dataplatform.mds.spread.column.IColumn;
import jp.co.yahoo.dataplatform.mds.spread.column.ColumnType;
import jp.co.yahoo.dataplatform.mds.spread.column.PrimitiveColumn;
import jp.co.yahoo.dataplatform.mds.spread.analyzer.IColumnAnalizeResult;

import jp.co.yahoo.dataplatform.schema.objects.FloatObj;
import jp.co.yahoo.dataplatform.schema.objects.PrimitiveType;
import jp.co.yahoo.dataplatform.schema.objects.PrimitiveObject;

import jp.co.yahoo.dataplatform.mds.binary.ColumnBinary;
import jp.co.yahoo.dataplatform.mds.binary.ColumnBinaryMakerConfig;
import jp.co.yahoo.dataplatform.mds.binary.ColumnBinaryMakerCustomConfigNode;
import jp.co.yahoo.dataplatform.mds.blockindex.BlockIndexNode;
import jp.co.yahoo.dataplatform.mds.inmemory.IMemoryAllocator;

public class DumpFloatColumnBinaryMaker implements IColumnBinaryMaker{

  private int getBinaryLength( final int columnSize ){
    return ( PrimitiveByteLength.INT_LENGTH * 2 ) + columnSize + ( columnSize * PrimitiveByteLength.FLOAT_LENGTH );
  }

  @Override
  public ColumnBinary toBinary(final ColumnBinaryMakerConfig commonConfig , final ColumnBinaryMakerCustomConfigNode currentConfigNode , final IColumn column , final MakerCache makerCache ) throws IOException{
    ColumnBinaryMakerConfig currentConfig = commonConfig;
    if( currentConfigNode != null ){
      currentConfig = currentConfigNode.getCurrentConfig();
    }
    byte[] binaryRaw = new byte[ getBinaryLength( column.size() ) ];
    ByteBuffer lengthBuffer = ByteBuffer.wrap( binaryRaw );
    lengthBuffer.putInt( column.size() );
    lengthBuffer.putInt( column.size() * PrimitiveByteLength.FLOAT_LENGTH );

    ByteBuffer nullFlagBuffer = ByteBuffer.wrap( binaryRaw , PrimitiveByteLength.INT_LENGTH * 2 , column.size() );
    FloatBuffer floatBuffer = ByteBuffer.wrap( binaryRaw , ( PrimitiveByteLength.INT_LENGTH * 2 + column.size() ) , ( column.size() * PrimitiveByteLength.FLOAT_LENGTH ) ).asFloatBuffer();

    int rowCount = 0;
    for( int i = 0 ; i < column.size() ; i++ ){
      ICell cell = column.get(i);
      if( cell.getType() == ColumnType.NULL ){
        nullFlagBuffer.put( (byte)1 );
        floatBuffer.put( (float)0 );
      }
      else{
        rowCount++;
        PrimitiveCell byteCell = (PrimitiveCell) cell;
        nullFlagBuffer.put( (byte)0 );
        floatBuffer.put( byteCell.getRow().getFloat() );
      }
    }

    byte[] binary = currentConfig.compressorClass.compress( binaryRaw , 0 , binaryRaw.length );

    return new ColumnBinary( this.getClass().getName() , currentConfig.compressorClass.getClass().getName() , column.getColumnName() , ColumnType.FLOAT , rowCount , binaryRaw.length , rowCount * PrimitiveByteLength.FLOAT_LENGTH , -1 , binary , 0 , binary.length , null );
  }

  @Override
  public int calcBinarySize( final IColumnAnalizeResult analizeResult ){
    return getBinaryLength( analizeResult.getColumnSize() );
  }

  @Override
  public IColumn toColumn( final ColumnBinary columnBinary , final IPrimitiveObjectConnector primitiveObjectConnector ) throws IOException{
    return new LazyColumn( columnBinary.columnName , columnBinary.columnType , new FloatColumnManager( columnBinary , primitiveObjectConnector ) );
  }

  @Override
  public void loadInMemoryStorage( final ColumnBinary columnBinary , final IMemoryAllocator allocator ) throws IOException{
    loadInMemoryStorage( columnBinary , columnBinary.binaryStart , columnBinary.binaryLength , allocator );
  }

  public void loadInMemoryStorage( final ColumnBinary columnBinary , final int start , final int length , final IMemoryAllocator allocator ) throws IOException{
    ICompressor compressor = FindCompressor.get( columnBinary.compressorClassName );
    byte[] binary = compressor.decompress( columnBinary.binary , start , length );
    ByteBuffer wrapBuffer = ByteBuffer.wrap( binary );
    int nullFlagBinaryLength = wrapBuffer.getInt();
    int floatBinaryLength = wrapBuffer.getInt();
    int nullFlagBinaryStart = PrimitiveByteLength.INT_LENGTH * 2; 
    int floatBinaryStart = nullFlagBinaryStart + nullFlagBinaryLength;

    ByteBuffer nullFlagBuffer = ByteBuffer.wrap( binary , nullFlagBinaryStart , nullFlagBinaryLength );
    FloatBuffer floatBuffer = ByteBuffer.wrap( binary , floatBinaryStart , floatBinaryLength ).asFloatBuffer();
    for( int i = 0 ; i < nullFlagBinaryLength ; i++ ){
      if( nullFlagBuffer.get() == (byte)0 ){
        allocator.setFloat( i , floatBuffer.get( i ) );
      }
    }
    allocator.setValueCount( nullFlagBinaryLength );
  }

  @Override
  public void setBlockIndexNode( final BlockIndexNode parentNode , final ColumnBinary columnBinary ) throws IOException{
    parentNode.getChildNode( columnBinary.columnName ).disable();
  }

  public class FloatDicManager implements IDicManager{

    private final IPrimitiveObjectConnector primitiveObjectConnector;
    private final byte[] nullBuffer;
    private final int nullBufferStart;
    private final int nullBufferLength;
    private final FloatBuffer dicBuffer;

    public FloatDicManager( final IPrimitiveObjectConnector primitiveObjectConnector , final byte[] nullBuffer , final int nullBufferStart , final int nullBufferLength , final FloatBuffer dicBuffer ){
      this.primitiveObjectConnector = primitiveObjectConnector;
      this.nullBuffer = nullBuffer;
      this.nullBufferStart = nullBufferStart;
      this.nullBufferLength = nullBufferLength;
      this.dicBuffer = dicBuffer;
    }

    @Override
    public PrimitiveObject get( final int index ) throws IOException{
      if( nullBuffer[index+nullBufferStart] == (byte)1 ){
        return null;
      }
      return primitiveObjectConnector.convert( PrimitiveType.FLOAT , new FloatObj( dicBuffer.get( index ) ) );
    }

    @Override
    public int getDicSize() throws IOException{
      return nullBufferLength;
    }

  }

  public class FloatColumnManager implements IColumnManager{

    private final IPrimitiveObjectConnector primitiveObjectConnector;
    private final ColumnBinary columnBinary;
    private final int binaryStart;
    private final int binaryLength;
    private PrimitiveColumn column;
    private boolean isCreate;

    public FloatColumnManager( final ColumnBinary columnBinary , final IPrimitiveObjectConnector primitiveObjectConnector ) throws IOException{
      this.columnBinary = columnBinary;
      this.primitiveObjectConnector = primitiveObjectConnector;
      this.binaryStart = columnBinary.binaryStart;
      this.binaryLength = columnBinary.binaryLength;
    }

    public FloatColumnManager( final ColumnBinary columnBinary , final IPrimitiveObjectConnector primitiveObjectConnector , final int binaryStart , final int binaryLength ) throws IOException{
      this.columnBinary = columnBinary;
      this.primitiveObjectConnector = primitiveObjectConnector;
      this.binaryStart = binaryStart;
      this.binaryLength = binaryLength;
    }

    private void create() throws IOException{
      if( isCreate ){
        return;
      }

      ICompressor compressor = FindCompressor.get( columnBinary.compressorClassName );
      byte[] binary = compressor.decompress( columnBinary.binary , binaryStart , binaryLength );
      ByteBuffer wrapBuffer = ByteBuffer.wrap( binary );
      int nullFlagBinaryLength = wrapBuffer.getInt();
      int floatBinaryLength = wrapBuffer.getInt();
      int nullFlagBinaryStart = PrimitiveByteLength.INT_LENGTH * 2;
      int floatBinaryStart = nullFlagBinaryStart + nullFlagBinaryLength;

      FloatBuffer floatBuffer = ByteBuffer.wrap( binary , floatBinaryStart , floatBinaryLength ).asFloatBuffer();

      IDicManager dicManager = new FloatDicManager( primitiveObjectConnector , binary , nullFlagBinaryStart , nullFlagBinaryLength , floatBuffer );
      column = new PrimitiveColumn( columnBinary.columnType , columnBinary.columnName );
      column.setCellManager( new BufferDirectCellManager( ColumnType.FLOAT , dicManager , nullFlagBinaryLength ) );

      isCreate = true;
    }

    @Override
    public IColumn get(){
      try{
        create();
      }catch( IOException e ){
        throw new UncheckedIOException( e );
      }
      return column;
    }

    @Override
    public List<String> getColumnKeys(){
      return new ArrayList<String>();
    }

    @Override
    public int getColumnSize(){
      return 0;
    }

  }

}

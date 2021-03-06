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
package jp.co.yahoo.dataplatform.mds.spread.flatten;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import jp.co.yahoo.dataplatform.mds.spread.Spread;
import jp.co.yahoo.dataplatform.mds.spread.column.IColumn;

public class FlattenFunction implements IFlattenFunction{

  private final List<FlattenColumn> flattenColumnList = new ArrayList<FlattenColumn>();
  private final Map<String,FlattenColumn> flattenColumnMap = new HashMap<String,FlattenColumn>(); 
  private final List<String> filterColumnList = new ArrayList<String>();

  public boolean isEmpty(){
    return flattenColumnList.isEmpty();
  }

  public int size(){
    return flattenColumnList.size();
  }

  public void add( final FlattenColumn flattenColumn ){
    if( ! flattenColumnMap.containsKey( flattenColumn.getLinkName() ) ){
      flattenColumnList.add( flattenColumn );
      flattenColumnMap.put( flattenColumn.getLinkName() , flattenColumn );
    }
  }

  private Spread allRead( final Spread spread ){
    Spread newSpread = new Spread();
    for( FlattenColumn flattenColumn : flattenColumnList ){
      IColumn column = flattenColumn.getColumn( spread );
      newSpread.addColumn( column );
    }
    newSpread.setRowCount( spread.size() );

    return newSpread;
  }

  private Spread filterRead( final Spread spread ){
    Spread newSpread = new Spread();
    for( String linkName : filterColumnList ){
      IColumn column = flattenColumnMap.get( linkName ).getColumn( spread );
      newSpread.addColumn( column );
    }
    newSpread.setRowCount( spread.size() );

    return newSpread;
  }

  @Override
  public boolean isFlatten(){
    return true;
  }

  @Override
  public Spread flatten( final Spread spread ){
    if( filterColumnList.isEmpty() ){
      return allRead( spread );
    }
    else{
      return filterRead( spread );
    }
  }

  @Override
  public String[] getFlattenColumnName( final String linkColumnName ){
    if( flattenColumnMap.containsKey( linkColumnName ) ){
      filterColumnList.add( linkColumnName );
      return flattenColumnMap.get( linkColumnName).getFilterColumnNameArray();
    }
    return new String[0];
  }

}

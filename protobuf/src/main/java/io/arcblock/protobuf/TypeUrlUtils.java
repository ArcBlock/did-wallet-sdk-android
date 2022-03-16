package io.arcblock.protobuf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Author       :paperhuang
 * Time         :2019/3/5
 * Edited By    :
 * Edited Time  :
 **/
//public class TypeUrlUtils {
//  public static Class getClazzByUrl(String typeUrl){
//    switch (typeUrl){
//      case TypeUrls.ACCOUNT_MIGRATE:
//       return Tx.AccountMigrateTx.class;
//      case TypeUrls.CREATE_ASSET:
//        return Tx.CreateAssetTx.class;
//      case TypeUrls.CONSENSUE_UPGRADE:
//        return Tx.ConsensusUpgradeTx.class;
//      case TypeUrls.DECLARE:
//        return Tx.DeclareTx.class;
//      case TypeUrls.DECLARE_FILE:
//        return Tx.DeclareFileTx.class;
//      case TypeUrls.EXCHANGE:
//        return Tx.ExchangeTx.class;
//      case TypeUrls.STAKE:
//        return Tx.StakeTx.class;
//      case TypeUrls.SYS_UPGRADE:
//        return Tx.SysUpgradeTx.class;
//      case TypeUrls.TRANSFER:
//        return Tx.TransferTx.class;
//      case TypeUrls.UPDATE_ASSET:
//        return Tx.UpdateAssetTx.class;
//
//      case TypeUrls.ACCOUNT_STATE:
//        return State.AccountState.class;
//      case TypeUrls.ASSET_STATE:
//        return State.AssetState.class;
//      case TypeUrls.FORGE_STATE:
//        return State.ForgeState.class;
//      case TypeUrls.STAKE_STATE:
//        return State.StakeState.class;
//      case TypeUrls.STATISTICS_STATE:
//        return State.StatisticsState.class;
//        //todo stake and others
//      default:
//        return Tx.TransferTx.class;
//    }
//  }
//
//  public static  <T> T decodeItx(Class<T> clazz){
//    try {
//      Method t = clazz.getDeclaredMethod("getDefaultInstance");
//      return  (T)t.invoke(null);
//    } catch (NoSuchMethodException e) {
//      e.printStackTrace();
//    } catch (IllegalAccessException e) {
//      e.printStackTrace();
//    } catch (InvocationTargetException e) {
//      e.printStackTrace();
//    }
//    return null;
//  }
//}

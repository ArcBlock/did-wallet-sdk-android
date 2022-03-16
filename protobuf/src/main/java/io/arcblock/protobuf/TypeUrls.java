package io.arcblock.protobuf;

/**
 * Author       :paperhuang
 * Time         :2019/2/22
 * Edited By    :
 * Edited Time  :
 **/
public class TypeUrls {
  public static final String ACCOUNT_MIGRATE = "fg:t:account_migrate";
  public static final String CREATE_ASSET = "fg:t:create_asset";
  public static final String CONSUME_ASSET = "fg:t:consume_asset";
  public static final String POKE = "fg:t:poke";

  public static final String CONSENSUE_UPGRADE = "fg:t:consensus_upgrade";
  public static final String DECLARE = "fg:t:declare";
  public static final String DECLARE_FILE = "fg:t:declare_file";
  public static final String EXCHANGE = "fg:t:exchange";
  public static final String EXCHANGE_V2 = "fg:t:exchange_v2";
  public static final String STAKE = "fg:t:stake";
  public static final String REVOKE_STAKE = "fg:t:revoke_stake";
  public static final String CLAIM_STAKE = "fg:t:claim_stake";
  public static final String SYS_UPGRADE = "fg:t:sys_upgrade";
  public static final String TRANSFER = "fg:t:transfer";
  public static final String TRANSFER_V2 = "fg:t:transfer_v2";
  public static final String TRANSFER_V3 = "fg:t:transfer_v3";
  public static final String UPDATE_ASSET = "fg:t:update_asset";
  public static final String ACQUIRE_ASSET ="fg:t:acquire_asset";
  public static final String ACQUIRE_ASSET_V2 ="fg:t:acquire_asset_v2";
  public static final String ACQUIRE_ASSET_V3 ="fg:t:acquire_asset_v3";
  public static final String DEPOSIT_TETHER= "fg:t:deposit_tether";
  public static final String EXCHANGE_TETHER= "fg:t:exchange_tether";
  public static final String DELEGATE= "fg:t:delegate";
  public static final String REVOKE_DELEGATE= "fg:t:revoke_delegate";

  public static final String APPROVE_WITHDRAW= "fg:t:approve_withdraw";
  public static final String DEPOSIT_TOKEN= "fg:t:deposit_token";
  public static final String REVOKE_WITHDRAW= "fg:t:revoke_withdraw";
  public static final String WITHDRAW_TOKEN= "fg:t:withdraw_token";
  public static final String CREATE_TOKEN = "fg:t:create_token";
  public static final String CREATE_FACTORY = "fg:t:create_factory";
  public static final String CREATE_ROLLUP = "fg:t:create_rollup";

  // forge state
  public static final String ACCOUNT_STATE = "fg:s:account";
  public static final String ASSET_STATE = "fg:s:asset";
  public static final String FORGE_STATE = "fg:s:forge";
  public static final String STAKE_STATE = "fg:s:stake";
  public static final String STATISTICS_STATE = "fg:s:statistics";

  // forge tx stake
  public static final String STAKE_FOR_NODE = "fg:x:stake_node";
  public static final String STAKE_FOR_USER = "fg:x:stake_user";
  public static final String STAKE_FOR_ASSET = "fg:x:stake_asset";
  public static final String STAKE_FOR_CHAIN = "fg:x:stake_chain";

  //
  public static final String TRANSACTION = "fg:x:tx";
  public static final String TRANSACTION_INFO = "fg:x:tx_info";
  public static final String TX_STATUS = "fg:x:tx_status";
  public static final String ADDRESS = "fg:x:account_migrate";
  public static final String EVENT_ADDRESS = "ec:x:event_address";
  public static final String EVENT_INFO = "ec:s:event_info";
  public static final String GENERIC_TICKET = "ec:s:general_ticket";
  public static final String TICKET_INFO = "ec:s:ticket_info";

  public static final String WORKSHOP = "ws:x:workshop_asset";

  public static String getTitleByTypeUrl(String typeUrl) {

    if (null == typeUrl) {
      return "Transaction";
    }

    switch (typeUrl) {
      case ACCOUNT_MIGRATE:
        return "Migrate";
      case CREATE_ASSET:
        return "Create Asset";
      case CONSUME_ASSET:
        return "Consume Asset";
      case POKE:
        return "Poke";
      case CONSENSUE_UPGRADE:
        return "Consensue Upgrade";
      case DECLARE:
        return "Declare";
      case DECLARE_FILE:
        return "Declare File";
      case EXCHANGE:
        return "Exchange";
      case STAKE:
        return "Stake";
      case SYS_UPGRADE:
        return "Sys Upgrade";
      case TRANSFER:
        return "Transfer";
      case UPDATE_ASSET:
        return "Update Asset";
      default:
        break;
    }

    return "Transaction";
  }
}

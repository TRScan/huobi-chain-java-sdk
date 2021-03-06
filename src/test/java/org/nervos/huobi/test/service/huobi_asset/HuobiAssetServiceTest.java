package org.nervos.huobi.test.service.huobi_asset;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.nervos.huobi.Huobi;
import org.nervos.huobi.service.governance.GovernanceService;
import org.nervos.huobi.service.governance.type.ConsumedTxFee;
import org.nervos.huobi.service.huobi_asset.HuobiAssetService;
import org.nervos.huobi.service.huobi_asset.type.*;
import org.nervos.muta.client.Client;
import org.nervos.muta.client.type.MutaRequestOption;
import org.nervos.muta.client.type.ParsedEvent;
import org.nervos.muta.client.type.primitive.Hash;
import org.nervos.muta.client.type.primitive.Hex;
import org.nervos.muta.client.type.primitive.U64;
import org.nervos.muta.wallet.Account;

@Slf4j
@Data
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HuobiAssetServiceTest {
    private static String ASSET_NAME = "Hamster Coin";
    private static String ASSET_SYMBOL = "HAM";
    private static U64 ASSET_SUPPLY = U64.fromBigInteger(BigInteger.valueOf(1000L));
    private static Account admin =
            Account.fromHexString(
                    "0x0000000000000000000000000000000000000000000000000000000000000001");
    private static Account someOneElse =
            Account.fromHexString(
                    "0x0000000000000000000000000000000000000000000000000000000000000002");
    private static HuobiAssetService huobiAssetService =
            new HuobiAssetService(
                    new Huobi(
                            Client.defaultClient(),
                            admin,
                            MutaRequestOption.defaultMutaRequestOption()));
    private static Hash asset_id;

    @Test
    @Order(1)
    public void createAsset() throws IOException {
        List<ParsedEvent<?>> events = new ArrayList<>();

        List<IssuerWithBalance> issuerWithBalances = new ArrayList<>();
        issuerWithBalances.add(new IssuerWithBalance(admin.getAddress(), ASSET_SUPPLY));
        Asset asset =
                huobiAssetService.create_asset(
                        new CreateAssetPayload(
                                ASSET_NAME,
                                ASSET_SYMBOL,
                                admin.getAddress(),
                                ASSET_SUPPLY,
                                issuerWithBalances,
                                U64.fromLong(1),
                                true),
                        events);

        asset_id = asset.getId();

        Assertions.assertEquals(ASSET_SUPPLY, asset.getSupply());
        Assertions.assertEquals(admin.getAddress(), asset.getAdmin());
        Assertions.assertEquals(ASSET_SYMBOL, asset.getSymbol());
        Assertions.assertTrue(
                events.stream()
                        .anyMatch(
                                parsedEvent ->
                                        parsedEvent.isMatch(
                                                HuobiAssetService.SERVICE_NAME,
                                                HuobiAssetService.EVENT_CREATE_ASSET)));

        Assertions.assertTrue(
                events.stream()
                        .anyMatch(
                                parsedEvent ->
                                        parsedEvent.isMatch(
                                                GovernanceService.SERVICE_NAME,
                                                ConsumedTxFee.name)));
    }

    @Test
    @Order(2)
    public void mintAsset() throws IOException {
        List<ParsedEvent<?>> events = new ArrayList<>();

        huobiAssetService.mint(
                new MintAssetPayload(
                        asset_id,
                        admin.getAddress(),
                        U64.fromLong(1000),
                        Hex.fromHexString("0x00"),
                        "memo"),
                events);

        Assertions.assertTrue(
                events.stream()
                        .anyMatch(
                                parsedEvent ->
                                        parsedEvent.isMatch(
                                                GovernanceService.SERVICE_NAME,
                                                ConsumedTxFee.name)));

        GetBalanceResponse getBalanceResponse =
                huobiAssetService.get_balance(new GetBalancePayload(asset_id, admin.getAddress()));

        Assertions.assertEquals(getBalanceResponse.getBalance(), U64.fromLong(2000));
    }

    @Test
    @Order(3)
    public void approveAsset() throws IOException {
        List<ParsedEvent<?>> events = new ArrayList<>();

        huobiAssetService.approve(
                new ApprovePayload(
                        asset_id, someOneElse.getAddress(), U64.fromLong(100), "test approve"),
                events);

        Assertions.assertTrue(
                events.stream()
                        .anyMatch(
                                parsedEvent ->
                                        parsedEvent.isMatch(
                                                GovernanceService.SERVICE_NAME,
                                                ConsumedTxFee.name)));
    }

    @Test
    @Order(4)
    public void transfer() throws IOException {
        List<ParsedEvent<?>> events = new ArrayList<>();

        huobiAssetService.transfer(
                new TransferPayload(
                        asset_id, someOneElse.getAddress(), U64.fromLong(1000), "test approve"),
                events);
        GetBalanceResponse getBalanceResponse_admin =
                huobiAssetService.get_balance(new GetBalancePayload(asset_id, admin.getAddress()));
        Assertions.assertEquals(U64.fromLong(1000), getBalanceResponse_admin.getBalance());

        GetBalanceResponse getBalanceResponse_someone =
                huobiAssetService.get_balance(
                        new GetBalancePayload(asset_id, someOneElse.getAddress()));
        Assertions.assertEquals(U64.fromLong(1000), getBalanceResponse_someone.getBalance());
    }

    @Test
    @Order(5)
    public void burnAsset() throws IOException {
        List<ParsedEvent<?>> events = new ArrayList<>();

        huobiAssetService.burn(
                new BurnAssetPayload(
                        asset_id, U64.fromLong(1500), Hex.fromHexString("0x00"), "test approve"),
                events);

        GetBalanceResponse getBalanceResponse =
                huobiAssetService.get_balance(new GetBalancePayload(asset_id, admin.getAddress()));

        System.out.println(getBalanceResponse.getBalance());

        Asset result = huobiAssetService.get_asset(new GetAssetPayload(asset_id));
        System.out.println(result.getSupply());
    }
}

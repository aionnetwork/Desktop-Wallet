package org.aion.wallet.connector;

import org.aion.api.IAionAPI;
import org.aion.api.IContract;
import org.aion.api.impl.internal.ApiUtils;
import org.aion.api.sol.IDynamicBytes;
import org.aion.api.sol.ISolidityArg;
import org.aion.api.sol.impl.Uint;
import org.aion.api.type.ApiMsg;
import org.aion.api.type.ContractResponse;
import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.AionConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ATSManager implements TokenManager{

    private static final String SEND = "send";
    private static final String BALANCE = "balanceOf";
    private static final String NAME = "name";
    private static final String SYMBOL = "symbol";
    private static final String GRANULARITY = "granularity";
    private static final String ABI_JSON = "ats_abi.json";

    private final Map<Address, IContract> addressToContract = new HashMap<>();
    private final String abiDescription = getAbiDescription();
    private final IAionAPI api;

    public ATSManager(final IAionAPI api) {
        this.api = api;
    }

    private String getAbiDescription() {
        final InputStream abiStream = this.getClass().getResourceAsStream(ABI_JSON);
        final StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(abiStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultStringBuilder.toString();
    }

    public final String getName(final String tokenAddress, final String accountAddress) throws ValidationException {
        final ContractResponse response = callTokenFunction(tokenAddress, accountAddress, NAME);
        return getStringResponse(response);
    }

    public final String getSymbol(final String tokenAddress, final String accountAddress) throws ValidationException {
        final ContractResponse response = callTokenFunction(tokenAddress, accountAddress, SYMBOL);
        return getStringResponse(response);
    }

    public final long getGranularity(final String tokenAddress, final String accountAddress) throws ValidationException {
        final ContractResponse response = callTokenFunction(tokenAddress, accountAddress, GRANULARITY);
        return getResponseContent(response);
    }

    public final BigInteger getBalance(final String tokenAddress, final String accountAddress) throws ValidationException {
        final ISolidityArg address = getSolidityAddress(accountAddress);
        final ContractResponse response = callTokenFunction(tokenAddress, accountAddress, BALANCE, address);
        return getBigIntegerResponse(response);
    }

    private ContractResponse callTokenFunction(
            final String tokenAddress,
            final String accountAddress,
            final String functionName,
            final ISolidityArg... parameters
    ) throws ValidationException {
        final IContract function = getTokenAtAddress(tokenAddress, accountAddress).newFunction(functionName);
        for (final ISolidityArg parameter : parameters) {
            function.setParam(parameter);
        }
        final ApiMsg nameResponse;
        nameResponse = function.build().execute();
        if (nameResponse.isError()) {
            throw new ValidationException(nameResponse.getErrString());
        }
        return nameResponse.getObject();
    }

    public final byte[] getEncodedSendTokenData(
            final String tokenAddress,
            final String accountAddress,
            final String destinationAddress,
            final BigInteger value
    ) {
        final IContract contract = getTokenAtAddress(tokenAddress, accountAddress);
        return contract.newFunction(SEND)
                .setParam(getSolidityAddress(destinationAddress))
                .setParam(getSolidityUInt(value))
                .setParam(IDynamicBytes.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY))
                .setFrom(Address.wrap(accountAddress))
                .setTxNrgLimit(AionConstants.DEFAULT_TOKEN_NRG)
                .setTxNrgPrice(AionConstants.DEFAULT_NRG_PRICE.longValue())
                .build().getEncodedData()
                .getData();
    }

    private IContract getTokenAtAddress(final String tokenAddressString, final String accountAddressString) {
        final Address tokenAddress = Address.wrap(tokenAddressString);
        final Address accountAddress = Address.wrap(accountAddressString);
        return addressToContract.computeIfAbsent(tokenAddress, s -> api.getContractController()
                .getContractAt(accountAddress, tokenAddress, abiDescription));
    }

    private BigInteger getBigIntegerResponse(final ContractResponse response) throws ValidationException {
        final byte[] typeResponse = getResponseContent(response);
        final BigInteger bigIntResponse = TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(typeResponse));
        if (BigInteger.ZERO.compareTo(bigIntResponse) > 0) {
            throw new ValidationException("Invalid token balance: " + bigIntResponse);
        }
        return bigIntResponse;
    }

    private String getStringResponse(final ContractResponse response) throws ValidationException {
        final String stringResponse = getResponseContent(response);
        if (stringResponse.isEmpty()) {
            throw new ValidationException("No token found!");
        }
        return stringResponse;
    }

    private <T> T getResponseContent(final ContractResponse response) throws ValidationException {
        final List<Object> data = response.getData();
        final Optional<Object> content = data.stream().findFirst();
        if (content.isPresent()) {
            return (T) content.get();
        } else {
            throw new ValidationException("Invalid Token call");
        }
    }

    private org.aion.api.sol.impl.Address getSolidityAddress(final String accountAddress) {
        return org.aion.api.sol.impl.Address.copyFrom(TypeConverter.StringHexToByteArray(accountAddress));
    }

    private org.aion.api.sol.impl.Uint getSolidityUInt(final BigInteger nr) {
        if (nr.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
            return Uint.copyFrom(ApiUtils.toHexPadded16(TypeConverter.StringHexToByteArray(TypeConverter.toJsonHex(nr))));
        } else {
            return Uint.copyFrom(nr.longValueExact());
        }
    }
}

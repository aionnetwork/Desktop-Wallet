const { default: LedgerNode } = require('@ledgerhq/hw-transport-node-hid');

describe('', () => {
  let dongle;

  before(async () => {
    const paths = await LedgerNode.list();
    dongle = await LedgerNode.open(paths[0]);
  });

  after(async () => {
    await dongle.close();
  });

  it('ledger interaction', async () => {
	
	var input = process.argv[4];

    const [signature] = await exchange(dongle, Buffer.from(input, 'hex'));
    console.log(`response `, signature.toString('hex'));
  });

});

async function exchange(dongle, apdu) {
  const response = await dongle.exchange(apdu);
  return [response.slice(0, response.length - 2), response.slice(response.length - 2)];
}

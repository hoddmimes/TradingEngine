function transfer( pDestination, pAmount ) {
    console.log('transfer');
    personal.unlockAccount( eth.accounts[0],'test');
    eth.sendTransaction({from: eth.accounts[0], to: pDestination, value: web3.toWei( pAmount,"ether")})
    var b
    console.log( eth.accounts[0] + "   wei: " + eth.getBalance( eth.accounts[0]) + " eth: " + web3.fromWei( eth.getBalance( eth.accounts[0]), 'ether'));
    console.log( pDestination + "   wei: " + eth.getBalance( pDestination ) + " eth: " + web3.fromWei( eth.getBalance( pDestination), 'ether'));
}
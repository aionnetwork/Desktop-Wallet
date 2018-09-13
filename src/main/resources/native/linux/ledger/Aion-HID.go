package main

import (
    "github.com/zondax/ledger-goclient"
    "encoding/hex"
    "fmt"
    "flag"
)

func Get_Ledger() (ledger *ledger_goclient.Ledger) {
    ledger, err := ledger_goclient.FindLedger()
    fmt.Printf("err %s\n",err)
    return ledger
}

func main() () {
    ledger := Get_Ledger()

    input := flag.String("param","00","param defaults to 00")
    flag.Parse()

    value := *input

    hexString, err := hex.DecodeString(value)
    output, err := ledger.Exchange(hexString)

    if err != nil { 
        fmt.Printf("error %s\n",err)
    }else{
        fmt.Printf("values %x\n",output)
    }
    
}
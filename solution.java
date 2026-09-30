// Multi-step loop
byte[] currentRequest = request;
while (true) {
    byte[] result = executeRemoteDroidGuard(currentRequest);
    if (isFinalResult(result)) {
        return result;
    }
    currentRequest = prepareNextStep(result);
}

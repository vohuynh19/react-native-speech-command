#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(SpeechCommand, RCTEventEmitter)

RCT_EXTERN_METHOD(initialize)
RCT_EXTERN_METHOD(stopClassifier)
RCT_EXTERN_METHOD(startClassifier)

@end

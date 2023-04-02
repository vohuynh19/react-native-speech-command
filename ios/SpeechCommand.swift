import TensorFlowLiteTaskAudio

struct Category: Codable{
    let label: String
    let score: Float
}

struct Result: Codable {
    let inferenceTime: Double
    let categories: [Category]
}

enum ModelType: String {
  case Yamnet = "YAMNet"
  case speechCommandModel = "Speech Command"

  var fileName: String {
    switch self {
    case .Yamnet:
      return "yamnet"
    case .speechCommandModel:
      return "speech_commands"
    }
  }
}

private let errorDomain = ""

@objc(SpeechCommand)
class SpeechCommand: RCTEventEmitter {
    private var modelType: ModelType = .speechCommandModel
    private var overlap = 0.5
    private var maxResults = 3
    private var threshold: Float = 0.0
    private var threadCount = 2
    private var errorDomain = ""
    
    private var classifier: AudioClassifier?
    private var audioRecord: AudioRecord?
    private var inputAudioTensor: AudioTensor?
    private var timer: Timer?
    private var processQueue = DispatchQueue(label: "processQueue")
    
    @objc
    func initialize() {
        if let bundleIdentifier = Bundle.main.bundleIdentifier {
            let components = bundleIdentifier.components(separatedBy: ".")
            if components.count >= 3 {
                let appDomain = [components[0], components[1], components[2]].joined(separator: ".")
                errorDomain = appDomain
            }
        }
        
        let modelFilename = modelType.fileName
        guard
          let modelPath = Bundle.main.path(
            forResource: modelFilename,
            ofType: "tflite"
          )
        else {
          print("Failed to load the model file \(modelFilename).tflite.")
          return
        }
        
        let classifierOptions = AudioClassifierOptions(modelPath: modelPath)
        classifierOptions.baseOptions.computeSettings.cpuSettings.numThreads = threadCount
        classifierOptions.classificationOptions.maxResults = maxResults
        classifierOptions.classificationOptions.scoreThreshold = threshold

        do {
          classifier = try AudioClassifier.classifier(options: classifierOptions)
          audioRecord = try classifier?.createAudioRecord()
          inputAudioTensor = classifier?.createInputAudioTensor()
        } catch {
          print("Failed to create the classifier with error: \(error.localizedDescription)")
          return
        }
    }
    
    @objc
    func stop() {
        audioRecord?.stop()
        timer?.invalidate()
        timer = nil
    }
    
    @objc
    func start() {
        if overlap < 0 {
          let error = NSError(
            domain: errorDomain,
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: "overlap must be equal or larger than 0."])
            sendEvent(withName: "onError", body: error)
        }

        if overlap >= 1 {
          let error = NSError(
            domain: errorDomain, code: 0,
            userInfo: [NSLocalizedDescriptionKey: "overlap must be smaller than 1."])
            sendEvent(withName: "onError", body: error)
        }

        do {
            
          try audioRecord?.startRecording()
          let audioFormat = inputAudioTensor?.audioFormat
          let lengthInMilliSeconds =
            Double(inputAudioTensor!.bufferSize)  / Double(audioFormat!.sampleRate)
            let interval = lengthInMilliSeconds * Double(1 - overlap)
            DispatchQueue.main.async(execute: {
                self.timer?.invalidate()
                self.timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) {
                    [weak self] _ in
                    self?.processQueue.async {
                        self?.runClassification()
                    }
                }
            })
        } catch {
            sendEvent(withName: "onError", body: error)
        }
      }
    
    private func runClassification() {
        let startTime = Date().timeIntervalSince1970
        do {
          try inputAudioTensor?.load(audioRecord: audioRecord!)
          let results = try classifier?.classify(audioTensor: inputAudioTensor!)
          let inferenceTime = Date().timeIntervalSince1970 - startTime
            
          let categories = results?.classifications[0].categories.map { category -> Category in
            let label = category.label ?? ""
            let score = category.score
            return Category(label: label, score: score)
          }
            
            let result = Result(
              inferenceTime: inferenceTime,
              categories: categories!
            )
            let encoder = JSONEncoder()
            encoder.nonConformingFloatEncodingStrategy = .convertToString(positiveInfinity: "Infinity", negativeInfinity: "-Infinity", nan: "NaN")
            
            let jsonData = try! encoder.encode(result)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            print(jsonString)
            self.sendEvent(withName: "onResults", body: jsonString)
            
        } catch {
          sendEvent(withName: "onError", body: error)
        }
      }
    
    override func supportedEvents() -> [String]! {
        return ["onResults", "onError"]
    }
    
    @objc
    static override func requiresMainQueueSetup() -> Bool {
        return true
    }
}

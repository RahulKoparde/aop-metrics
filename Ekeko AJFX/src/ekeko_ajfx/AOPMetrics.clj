(ns ekeko-ajfx.AOPMetrics
  ^{:doc "Specific Aspect-oriented programming Metrics"
    :author "Hakan Ozler - ozler.hakan[at]gmail.com" }
  (:refer-clojure :exclude [== type declare class])
  (:require [clojure.core.logic :as l]
            [clojure.core :as cr]
            [clojure.java.io :as io]
            [damp.ekeko.aspectj  
             [weaverworld :as w]  
             [soot :as ajsoot] 
             [ajdtastnode :as ajst] 
             [ajdt :as ajdt]]
            [damp.ekeko.soot     
             [soot :as jsoot]] 
            [damp.ekeko.jdt      
             [reification :as reif] 
             [javaprojectmodel :as jpm] 
             [basic :as bsc] 
             [astnode :as astn]])
  (:use [inspector-jay.core]
        [clojure.repl]
        [damp.ekeko logic]
        [damp.ekeko ekekomodel]
        [damp.ekeko gui]
        [damp.ekeko]
        [clojure.inspector :exclude [inspect]])
  (:import [soot.jimple IdentityStmt]
    [soot.jimple.internal JimpleLocal]
    [soot.jimple ThisRef ParameterRef]
    [org.aspectj.lang Signature]
    [java.lang Integer]
    [org.eclipse.jdt.core IJavaElement ITypeHierarchy IType IPackageFragment IClassFile ICompilationUnit
     IJavaProject WorkingCopyOwner IMethod]
    [org.eclipse.jdt.core.dom Expression IVariableBinding ASTParser AST IBinding Type TypeDeclaration 
     QualifiedName SimpleName ITypeBinding MethodDeclaration
     MethodInvocation ClassInstanceCreation SuperConstructorInvocation SuperMethodInvocation
     SuperFieldAccess FieldAccess ConstructorInvocation ASTNode ASTNode$NodeList CompilationUnit]
    [org.aspectj.weaver.patterns Pointcut AndPointcut]))

 (comment )

 ;############################### METRIC LOC ###############################
 ;count the number of lines of java code & aspect code in a given project - except blank lines (comments & javadocs are still counted)
 (defn class-loc [filePath ignoredTestName]
  (reduce
    +
    (for [file (file-seq  filePath) :when (and (.endsWith (.toString file )".java") (false? (lastIndexOfText (.toString file) ignoredTestName)))]
      (with-open [rdr (io/reader  file)] (count  (filter #(re-find #"\S" %) (line-seq rdr)))))))

;(class-loc (io/file "C:/Users/HAKAN/runtime-New_configuration-clojure/Clojure-1/src/"))
;(class-loc (io/file"C:/Users/HAKAN/Desktop/Thesis/ws/AJHotDraw/src/aspects"));36225
(class-loc (io/file "C:/Users/HAKAN/runtime-New_configuration-clojure/Clojure-1/src/") "MainTST")

 (defn aspect-loc [filePath]
  (reduce
    +
    (for [file (file-seq  filePath) :when (.endsWith (.toString file )".aj")]
      (with-open [rdr (io/reader file)] (count  (filter #(re-find #"\S" %) (line-seq rdr)))))))

;(aspect-loc (io/file "C:/Users/HAKAN/runtime-New_configuration-clojure/Clojure-1/src/"))
 ;(aspect-loc (io/file"C:/Users/HAKAN/Desktop/Thesis/ws/AJHotDraw/src/aspects"));2111
 ;(aspect-loc (io/file "C:/Users/HAKAN/workspace/PetstoreAspectJ/blueprints/petstore1.4/src"))
 (class-loc (io/file "C:/Users/HAKAN/runtime-New_configuration-clojure/Contract4J5/contract4j5/src"))
 ;############################### METRIC VS ###############################
 ;the number of JAVA classes (.java) in a selected project
 ;(defn class-count [filePath]
 ; (reduce
 ;   +
 ;   (for [file (file-seq filePath) :when (.endsWith (.toString file ) ".java")] 1) ))
 ;(class-count (io/file "C:/Users/HAKAN/runtime-New_configuration-clojure/Contract4J5/contract4j5/src"));

 ;Number of Classes in the project except enums, interfaces, their sub-classes!!, and a test class.
 (defn NOClasses [?classes]
            (l/fresh []
                  (w/class ?classes)
                  (equals false (or                                   
                          (.isEnum ?classes)
                          (IndexOfText (.getName ?classes) "$");this line excludes all sub-classes!
                          (lastIndexOfText (.getName ?classes) "MainTST");our initial main to activate soot analysis, so I ignore it
                          (IndexOfText (.getName ?classes) "lang.Object")))))
 
 (inspect  (sort-by first (ekeko [?cn] (l/fresh [?c] (NOClasses ?c) (equals ?cn (.getName ?c))))))
 (count (ekeko [?cn] (l/fresh [?c] (NOClasses ?c) (equals ?cn (.getName ?c)))))
 ;the number of aspects in a selected project -- include sub-aspects
 (defn NOAspects [?aspects ?source]  
   (l/fresh []
           (w/aspect ?aspects) 
           (equals ?source (.getSourceLocation ?aspects))));just to be sure!
 
 (inspect (sort-by first (ekeko [?an] (l/fresh [?as ?sour] (NOAspects ?as ?sour) (equals ?an (.getName ?as))))))
 (count (ekeko [?an] (l/fresh [?as ?sour] (NOAspects ?as ?sour) (equals ?an (.getName ?as)))))
 
 ;the number of aspectj (.aj) files in a project!  Just an additional query, it is not relating with the main query!  
 ;(defn aspect-count [filePath]
 ; (reduce
 ;   +
 ;   (for [file (file-seq filePath) :when (.endsWith (.toString file )".aj")] 1) ))
 ;(aspect-count (io/file "C:/Users/HAKAN/workspace/PetstoreAspectJ/blueprints/petstore1.4/src"))
 ;############################### METRIC NOAttributes-fields ###############################
 ;(inspect  (ekeko [?fields] (w/field ?fields)));get the entire fields from the weaver
 
 ;only works for aspects to count the number of fields!
 ;(inspect (ekeko [?fields] (ajdt/field ?fields)));Relation of fields declared ONLY within aspects
 ;(count (ekeko [?fields] (ajdt/field ?fields )));
 ;(inspect (ekeko [?aspect ?f] (ajdt/aspect-field ?aspect ?f)));same as ajdt/field; for aspect!

 ;get a specific class and its fields in terms of a package name
 (inspect (ekeko [?t ?f] 
                 (w/type-fields ?t ?f)
                 (succeeds (IndexOfText (.toString ?t) "InvariantTypeConditions"))))
 
;count-fields-in both CLASSES and ASPECTS except their sub-classes' fields!!
(defn count-fields-in-modules [?c ?f]
         (l/fresh []
                     (w/type-field ?c ?f)
                     (equals false (.isEnum ?c))
                     (equals false (.isInterface ?c))
                     (equals false (and (.isClass ?c) (IndexOfText  (.getName ?c) "$")))
                     (succeeds (empty? (filter #(re-matches #"\S+\$([1-9]|0)+" %)  [(.getName ?c)])))
                     (equals false (or 
                                     (.startsWith (.getName ?f) "ajc")
                                     (.startsWith (.getName ?f) "this")
                                     (IndexOfText (.getName ?f) "$")))))
                     
 (inspect (sort-by first (ekeko [?cn ?f] (l/fresh [?c] (count-fields-in-modules ?c ?f ) (equals ?cn (.getName ?c))))))
 (count (ekeko [?c ?f] (count-fields-in-modules ?c ?f))) 

 ;############################### METRIC NOOperations (methods and advices) ###############################
 ;METHODS: both all classes and aspects
 ;(inspect (ekeko [?methods] (w/method ?methods)))

 ;NUMBER OF METHODS: only responsible for classes!  - construct methods including 
 (defn classes-methods [?typesn ?methods]
                (l/fresh [?types]
                         (NOClasses ?types)
                         (w/type-method ?types ?methods)
                         (equals ?typesn (.getName ?types))
                         (succeeds (empty? (filter #(re-matches #"\S+\$\S+" %) [(.getName ?types)])))
                         (equals false (= "STATIC_INITIALIZATION" (.toString (.getKind ?methods))))
                         (succeeds (empty? (filter #(re-matches #"\S+\$([1-9]|0)+" %) [(.getName ?methods)])))))
                         ;(succeeds (empty? (filter #(re-matches #"\S+\$[1-9]+" %) [(.getName (.getDeclaringType ?methods))])))
                         ;(succeeds (IndexOfText (.toString (.getName ?types)) "org.contract4j5.reporter.Severity" ))))
 ;1 Classs
 (inspect (sort-by first (ekeko [?types ?m] (classes-methods ?types ?m))))
 (count ( ekeko [?types ?m] (classes-methods ?types ?m)))

 ;exclude construct methods of classes!
 (defn classes-methods-without-constructs [?t ?m]
             (l/fresh []
                  (classes-methods ?t ?m)
                  (equals false (= 3 (.getKey (.getKind ?m))))))
 ;1 Class -nonConstructs
 (inspect(sort-by first (ekeko [?t ?m] (classes-methods-without-constructs ?t ?m))))
 (count (ekeko [?t ?m] (classes-methods-without-constructs ?t ?m)))

 ;2 Aspect : find basic method declarations in aspect files
 (defn aspects-methods [?aspects ?methods]
                (l/fresh [?tname ?source ?methodName]
                        (NOAspects ?aspects ?source)
                        (w/type-method ?aspects ?methods)
                        (equals ?tname (.getClassName ?aspects))
                        (equals ?methodName (.getName ?methods))
                        (equals false (= 8 (.getModifiers ?methods)))
                        (equals false (.startsWith (.getName ?methods) "ajc"))
                        (equals false (or (= "hasAspect" (.getName ?methods)) 
                                          (= "aspectOf" (.getName ?methods)) 
                                          (= "<init>" (.getName ?methods))))))

 (inspect (sort-by first  (ekeko [?tn ?m] (l/fresh [?t] (aspects-methods ?t ?m) (equals ?tn (.getClassName ?t))))))
 (count (ekeko [?tn ?m] (l/fresh [?t] (aspects-methods ?t ?m) (equals ?tn (.getClassName ?t)))))
 ; <= without intertyped method declarations
 ;-------------------------------------------------------------
 ;    intertyped method declarations =>  METRIC NOOI (methods and advices and intertype methods)
 
 ;METHODS: only intertyped method declarations
 ;2.2 Aspect : get the all intertype method declaration implemented in a project
 (defn aspects-intertyped-methods [?decAspect ?methods]
                (l/fresh [?types]
                          (w/type-method ?types ?methods)
                          (succeeds   (.isAspect ?types))
                          (equals ?decAspect (.getName (.getDeclaringType ?methods)))
                          (succeeds   (.startsWith (.getName ?methods) "ajc$interMethod$"))));$AFTER,$BEFORE, so on...

 (inspect (sort-by first (ekeko [?decAspect ?methods] (aspects-intertyped-methods  ?decAspect ?methods))))
 (count (ekeko [?get ?decAspect] (aspects-intertyped-methods ?get ?decAspect)))
 
 ;2.2 Aspect get the all intertype method declaration implemented in a project except abstracts
 (defn intertype-methods [?i] 
                       (l/fresh [] 
                            (w/intertype|method ?i)
                            (equals false (.isAbstract (.getSignature ?i)))))
 
  (inspect (ekeko [?i] (intertype-methods ?i)))
  (count (ekeko [?i] (intertype-methods ?i)))
 ;COUNT: 1 class + 2 aspect + 2.2 aspect :RESULT combines with 

 ;-------------------------------------------------------------------------------------------------
 ;NUMBER OF ADVICES
(defn NOAdvices [?aspect ?adv]  
              (l/fresh []
                   (w/advice ?adv)
                   (equals ?aspect (.getConcreteAspect ?adv))
                   (equals false (IndexOfText (.getName (.getClass ?adv)) "Checker"))
                   (equals false (or
                                   (or (.isCflow (.getKind ?adv)) (.isPerEntry (.getKind ?adv)))
                                   (= "softener" (.getName (.getKind ?adv)))));I must exclude perThis, perTarget , perCflow, Cflow, CflowBelow, softener, and Checker(warning)!!
                   (equals false (= "" (.toString (.getPointcut ?adv))))))
                   ;(equals true (= (.getName ?aspect) (.getName (.getDeclaringType (.getSignature ?adv)))))))

(inspect (sort-by first (ekeko [?an ?adv] (l/fresh [?a] (NOAdvices ?a ?adv) (equals ?an (.toString ?a))))))
(count (ekeko [?a ?adv] (NOAdvices ?a ?adv)))

 ;############################### Advice-Method dependence (AM) :the number of method calls per advice body ###############################
 (defn get-soot-advice|method [?aspectName ?calledmethods ?soot|methodName]
               (l/fresh [?soot|method ?advice ?aspect ?soot]
                         (NOAdvices ?aspect ?advice)
                         (ajsoot/advice-soot|method ?advice ?soot|method)
                         (NOMethodCalls ?soot|method ?aspectName ?calledmethods ?soot|methodName)))
 
 (inspect (sort-by first (ekeko [?aspectName ?calledmethods ?soot|methodName] (get-soot-advice|method ?aspectName ?calledmethods ?soot|methodName))))
 (count (sort-by first (ekeko [?aspectName ?calledmethods ?soot|methodName] (get-soot-advice|method ?aspectName ?calledmethods ?soot|methodName)))) 
 ;############################### IntertypeMethod-Method dependence (IM) :the number of method calls per intertype method body ###############################
 (defn get-soot-intertype|method [?aspectName ?calledmethods ?soot|interName]
               (l/fresh [?soot ?itmethod ?units ?unit ?soot  ?inter|method ?soot|methodName]
                         (ajsoot/intertype|method-soot|method ?itmethod ?soot)
                         (succeeds (.hasActiveBody ?soot))
                         (equals ?units (.getUnits (.getActiveBody ?soot)))
                         (contains ?units ?unit)
                         (succeeds (.containsInvokeExpr ?unit))
             	           (succeeds (or (= "soot.jimple.internal.JAssignStmt" (.getName (.getClass ?unit)))  
                                       (= "soot.jimple.internal.JInvokeStmt" (.getName (.getClass ?unit)))))
                         (succeeds (.containsInvokeExpr ?unit))
                         (equals ?inter|method (.getMethod (.getInvokeExpr ?unit)))
                         (NOMethodCalls ?inter|method ?aspectName ?calledmethods ?soot|methodName)
                         (equals ?soot|interName (str "InterMethod {"(subs ?soot|methodName (+ (.lastIndexOf ?soot|methodName "$") 1))))))
 
 (inspect (sort-by first (ekeko [ ?aspectName ?calledmethods ?soot|interName ] (get-soot-intertype|method  ?aspectName ?calledmethods ?soot|interName ))))
 
 (inspect (ekeko [?soot]
                 (l/fresh [ ?itmethod]
                          (ajsoot/intertype|method-soot|method ?itmethod ?soot)
                          (equals true (= (.getName ?soot) "aspectUpdating")))))
 
 ;############################### Method-Method dependence (MM) :the number of method calls per method body declared in aspects ###############################
 (defn get-ajmethods-soot|method [ ?aspectName ?calledmethods ?soot|methodName]
               (l/fresh [?aspect ?ajmethod ?soot|method]
                        (aspects-methods ?aspect ?ajmethod)
                        (ajsoot/method-soot|method ?ajmethod ?soot|method);new function has been created in the Ekeko AspectJ project -> /EkekoAspectJ/src/damp/ekeko/aspectj/soot.clj Line: 113
                        (NOMethodCalls ?soot|method ?aspectName ?calledmethods ?soot|methodName)))
 
 (inspect (count (sort-by first (ekeko [ ?aspectName  ?soot|methodName ?calledmethods]   (get-ajmethods-soot|method  ?aspectName ?calledmethods ?soot|methodName)))))
 (count (ekeko [ ?aspectName  ?soot|methodName ?calledmethods] (get-ajmethods-soot|method  ?aspectName ?calledmethods ?soot|methodName)))
 ;-------------------------------------------------------------------------------------------------------------------------------------------------------------
 ;find the RELATED SootMethod(s)
 (inspect (ekeko [?m ?n]
  (jsoot/soot :method ?m)
  (equals ?n (.getName ?m))
  (equals true (IndexOfText ?n "OptimizedAbstractTimestampAspect"))))

 ;find all SootMethods
 (inspect (ekeko [?m ]
  (jsoot/soot :method ?m)))
  
 ;Additional query: get the entire advice soot methods of the selected advice package
 (defn get-advices-soot|methods [?soot|advicemethod ?units]
               (l/fresh [?adv ?aspect]
                         (NOAdvices ?aspect ?adv);I am using this function or I can also use ajdt/advice function to get the proper declarded advice in a project and to avoid the errors!
                         (ajsoot/advice-soot|method ?adv ?soot|advicemethod)
                         (succeeds (.hasActiveBody ?soot|advicemethod))
                         (equals ?units (.getUnits (.getActiveBody ?soot|advicemethod)))
                         (equals true (=  "org.contract4j5.configurator.ensurer.ReporterDependencyInitializer" (.getName (.getDeclaringClass ?soot|advicemethod))))))

 (inspect (ekeko [?soot|advicemethod ?units] (get-advices-soot|methods ?soot|advicemethod ?units))) 
 ;get the all transforming classes with a given part of a package name
 (inspect (ekeko [?model ?scne ?class]
             (l/fresh [?classes]
                      (jsoot/soot-model-scene ?model ?scne)
                      (equals ?classes (.getClasses ?scne))
                      (contains ?classes ?class)
                      (succeeds (IndexOfText (.getName ?class) "org.contract4j5.aspects.InvariantCtorConditions")))))

;################################## Main Function for the 3 Metrics, It is working.. (I hope so) ####################################
 (defn NOMethodCalls [?soot|method ?aspectName ?calledmethods ?soot|methodName]; ?method : class soot.SootMethod
          (l/fresh [?units ?sootMDeclass]
            (succeeds (.hasActiveBody ?soot|method))
            (soot|unit-getDeclarationClassname ?soot|method ?aspectName)
            (equals ?units (.getUnits (.getActiveBody ?soot|method)))
            (equals ?soot|methodName (str "Method {"(.getName ?soot|method)"}"))
            (contains ?units ?calledmethods)
            (succeeds (.containsInvokeExpr ?calledmethods))
	          (succeeds (or (= "soot.jimple.internal.JAssignStmt" (.getName (.getClass ?calledmethods)))  
	                        (= "soot.jimple.internal.JInvokeStmt" (.getName (.getClass ?calledmethods)))))	          
	          (equals ?sootMDeclass (.getShortName (.getDeclaringClass (.getMethod (.getInvokeExpr ?calledmethods)))))
	          (equals false (or  (IndexOfText  ?sootMDeclass "StringBuilder")
                               (IndexOfText  ?sootMDeclass "aspectj")
	                             (IndexOfText  ?sootMDeclass "apache")
                               (IndexOfText  ?sootMDeclass "CFlowCounter")
                               (IndexOfText  ?sootMDeclass "CFlowStack")))
	          (equals false (= "<init>" (.getName (.getMethod (.getInvokeExpr ?calledmethods)))))
            (equals false
                    (and
                      (.startsWith (.toString (.getInvokeExpr ?calledmethods)) "staticinvoke")
                      (IndexOfText (.getName  (.getMethod (.getInvokeExpr ?calledmethods))) "$")
                      (.containsInvokeExpr    (.getFirstNonIdentityStmt (.getActiveBody (.getMethod (.getValue (.getInvokeExprBox ?calledmethods))))))
                      (or
                        (.startsWith (.name (.getMethodRef (.getValue (.getInvokeExprBox (.getFirstNonIdentityStmt (.getActiveBody (.getMethod (.getValue (.getInvokeExprBox ?calledmethods))))))))) "ajc$set")
                        (.startsWith (.name (.getMethodRef (.getValue (.getInvokeExprBox (.getFirstNonIdentityStmt (.getActiveBody (.getMethod (.getValue (.getInvokeExprBox ?calledmethods))))))))) "ajc$get"))))
	          (equals false
		                (and
		                  (or
		                    (.startsWith (.toString (.getInvokeExpr ?calledmethods)) "staticinvoke")
		                    (.startsWith (.toString (.getInvokeExpr ?calledmethods)) "virtualinvoke"))
		                  (or
                         (=               (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "valueOf")
		                     (lastIndexOfText (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "$advice")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$afterReturning$")
		                     (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$around$")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$interFieldSetDispatch$")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$interFieldGetDispatch$")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$before$")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$after$")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "ajc$afterThrowing$")
                         (.startsWith     (.getName (.getMethod (.getInvokeExpr ?calledmethods))) "access$"))))
	          (equals false
                (or
                  (= "aspectOf" (.getName (.getMethod (.getValue (.getInvokeExprBox ?calledmethods)))))
                  (= "makeJP" (.getName (.getMethod (.getValue (.getInvokeExprBox ?calledmethods)))))))))
 
 ;############################### Attribute-Class dependence Measure (AtC) ###############################
 ;Definition : if a class is the type of an field of an aspect - - count the number of types that belong to fields in aspects
 ;Filtering primitive types and interfaces that could be the type of a field!
  (defn getField-AtC [?aspectName ?fieldName ?fieldTypeName] 
         (l/fresh [?field ?aspect ?isSameInterface ?signature ?fieldType]
                 (w/type-field ?aspect ?field)
                 (succeeds (.isAspect ?aspect))
                 (equals ?aspectName (str "Aspect {"(.getName ?aspect)"}"))
                 (equals ?fieldType  (.getType ?field))
                 (equals false       (.isPrimitiveType (.getType ?field))); I ignore primitive types such as boolean, int , void ,double, and so on.
                 (equals false       (or (.startsWith (.getName ?field) "ajc") (.startsWith (.getName ?field) "this")))
                 (equals ?fieldTypeName     (.getName ?fieldType))
                 (equals ?isSameInterface (getInterface ?fieldTypeName))
                 (equals true        (nil? ?isSameInterface));check whether the type is interface or not!!
                 (equals ?fieldName  (str "<Field name: " (.getName ?field) ">"))))
 
 (inspect (sort-by first (ekeko [?typef ?f ?t] (getField-AtC ?t ?f ?typef))))
 ;############################### Advice-Class  dependence (AC) ###############################
 ; if a class is  the type of a parameter of a piece of advice of an aspect 
 (defn getAC-p1 [?aspectSN ?adviceKind ?typename] 
   (l/fresh [?aspect ?typesofAdvice ?advice  ?isInterface ?parameter]
            (NOAdvices ?aspect ?advice)
            (equals ?aspectSN (.getSimpleName ?aspect))
            (equals   ?adviceKind (.getKind ?advice))
            (equals   ?typesofAdvice (.getParameterTypes (.getSignature ?advice)))
            (contains ?typesofAdvice ?parameter)
            (equals   ?typename  (.getName ?parameter))
            (equals   ?isInterface (getInterface ?typename));control whether a selected type is interface that was implemented in a given AspectJ app 
            (succeeds (nil? ?isInterface))
            (equals false (.isPrimitiveType ?parameter))))           
 
 (inspect  (sort-by first (ekeko [?as ?a ?r] (getAC-p1 ?as ?a ?r)))) 
 
 ; the return type of the piece of advice - around - ; "after returning" "after throwing" are being checked in the above function called -getAC-p1-
 (defn getAC-p2 [?aspectSN ?adviceKind ?returntypename] 
   (l/fresh [?aspect ?advice ?isInterface ?returntype]
            (NOAdvices ?aspect ?advice)
            (equals ?aspectSN (.getSimpleName ?aspect))
            (equals ?adviceKind (.getKind ?advice))
            (succeeds (= 5 (.getKey (.getKind ?advice))))
            (equals ?returntype (.getReturnType (.getSignature ?advice)))
            (equals ?returntypename (.getName ?returntype))
            (equals false (.isPrimitiveType ?returntype))
            (equals ?isInterface (getInterface ?returntypename))
            (succeeds  (nil? ?isInterface))))
  
  (inspect  (sort-by first (ekeko [?r ?as ?a] (getAC-p2 ?as ?a ?r))))
  
  ;combined the two queries in one inspect
  (inspect (sort-by first  (clojure.set/union
                            (ekeko [?vari ?as ?ad ] (getAC-p1 ?as ?ad ?vari))
                            (ekeko [?vari ?as ?ad ] (getAC-p2 ?as ?ad ?vari)))))
 ;############################### Intertype method-Class dependence (IC) ###############################
 ;if classes are the type of  parameters or return type of intertype method declarations in aspects of a given AspectJ App
 
 ;find all return types of intertype method declarations
 (defn measureIC-returnType [?aspect ?interName ?type ?returnname] 
         (l/fresh [?isInterface ?i ?return] 
                  (intertype-methods ?i)
                  (equals ?aspect (str "Aspect {"(.getName (.getAspectType ?i))"}"))
                  (equals ?return (.getReturnType (.getSignature ?i)))
                  (equals false (.isPrimitiveType ?return));except primitive types
                  (equals ?returnname  (.getName ?return))
                  (equals ?isInterface (getInterface ?returnname));except interfaces
                  (succeeds  (nil? ?isInterface))
                  (equals ?type (str "RETURN"))
                  (equals ?interName (str (.getClassName (.getDeclaringType (.getSignature (.getMunger ?i))))"."(.getName (.getSignature ?i))))))
 
 (inspect (sort-by first  (ekeko [?aspect ?interName ?type ?return] (measureIC-returnType ?aspect ?interName ?type ?return))))
 ;find all parameter types of intertype method declarations
 (defn measureIC-parameters [?aspect ?interName ?param ?variName] 
	       (l/fresh [?v ?isInterface ?i ?vari] 
	                (intertype-methods ?i)
	                (equals ?aspect (str "Aspect {"(.getName (.getAspectType ?i))"}"))
	                (equals ?v (.getParameterTypes (.getSignature ?i)))
	                (contains ?v ?vari)
	                (equals false (.isPrimitiveType ?vari));except primitive types
	                (equals ?variName  (.getName ?vari))
	                (equals ?isInterface (getInterface ?variName));except interfaces
	                (succeeds  (nil? ?isInterface))
	                (equals ?param (str "PARAM"))
	                (equals ?interName (str (.getClassName (.getDeclaringType (.getSignature (.getMunger ?i))))"."(.getName (.getSignature ?i))))))
 
 (inspect (sort-by first  (ekeko [?aspect ?interName ?type ?variName] (measureIC-parameters ?aspect ?interName ?type ?variName))))

  ;combined the two queries in one inspect
  (inspect (sort-by first (clojure.set/union
                            (ekeko [?vari ?as ?a ?t ] (measureIC-returnType ?as ?a ?t ?vari))
                            (ekeko [?vari ?as ?a ?t ] (measureIC-parameters ?as ?a ?t ?vari)))))
  
 ;############################### Method-Class dependence (MC) ###############################
 ;Definition: if classes are the type(s) of parameters or return type(s) of method declarations in aspects of a given AspectJ App
 (defn measureMC-param [?aspectn ?methodN ?paramName ?rtype] 
   (l/fresh [?aspect ?params ?method ?param ?isInterface]
     (aspects-methods ?aspect ?method)
     (equals ?aspectn (str "Aspect {"(.getSimpleName ?aspect) "}"))
     (equals ?params (.getParameterTypes ?method))
     (contains ?params ?param)
     (equals false (.isPrimitiveType ?param))
     (equals ?paramName (.getName ?param))
     (equals ?isInterface (getInterface ?paramName));except interface classes
     (succeeds  (nil? ?isInterface))
     (equals ?methodN (str "<Method Name: "(.getName ?method)">"))
     (equals ?rtype (str "PARAM"))))
  
 (inspect (sort-by first  (ekeko [?p ?A ?m ?r] (measureMC-param ?A ?m ?p ?r))))
  
 (defn measureMC-return [?aspectn ?methodN ?returnName ?rtype] 
   (l/fresh [?aspect ?return ?method ?isInterface]
     (aspects-methods ?aspect ?method)
     (equals ?aspectn (str "Aspect {"(.getSimpleName ?aspect) "}"))
     (equals ?return (.getReturnType ?method))
     (equals false (.isPrimitiveType ?return))
     (equals ?returnName (.getName ?return))
     (equals ?isInterface (getInterface ?returnName));except interface classes
     (succeeds  (nil? ?isInterface))
     (equals ?methodN (str "<Method Name: "(.getName ?method)">"))
     (equals ?rtype (str "RETURN"))))
  
  (inspect (sort-by first  (ekeko [?p ?A ?m ?r] (measureMC-return ?A ?m ?p ?r))))
   
  ;combined the two queries in one inspect
  (inspect (sort-by first (clojure.set/union
                            (ekeko [?vari ?A ?m ?r] (measureMC-param ?A ?m ?vari ?r))
                            (ekeko [?vari ?A ?m ?r] (measureMC-return ?A ?m ?vari ?r)))))
  
  ;############################### Pointcut-Class dependence (PC) ###############################
  ;if a class is the type of a parameter of a pointcut (poincutDefinition) in an aspect
  (defn measurePC [?typename ?pn ?aspect] 
    (l/fresh [?point ?types ?type ?isInterface]
             (w/pointcutdefinition ?point)
             (equals ?types (.getParameterTypes ?point))
             (contains ?types ?type)
             (equals false (.isPrimitiveType ?type))
             (equals ?typename (.getName ?type))
             (equals ?isInterface (getInterface ?typename))
             (succeeds  (nil? ?isInterface))
             (equals ?aspect (.getName (.getDeclaringType ?point)))
             (equals ?pn (str "<Pointcut Name :"(.getName ?point)">"))))
  
  (inspect (sort-by first (ekeko [?tn ?pn ?aspect] (measurePC ?tn ?pn ?aspect))))
  
  ;############################### Pointcut-Method dependence (PM) ###############################
  ;if a pointcut of an aspect contains at least one join point that is related to a method/construct of a class
 (defn countPM [?calledClass ?calledMth  ?aspectName ?adviceKind ?pointcut ] 
            (l/fresh [?toLongStringmethod ?methodName ?aspect ?advice ?shadow ?shadowParent ?fullyClassName ?class]
                    (NOAdvices ?aspect ?advice)
                    (w/advice-shadow ?advice ?shadow);in order to reach the join point shadows, I used w/advice-shadow to pick them up along with advices' pointcut
                    (equals true (.isCode ?shadow))
                    (equals ?shadowParent (.getParent (.getParent ?shadow)))
                    (succeeds  (or (.startsWith (.getName ?shadow) "method-call")
                                   (.startsWith (.getName ?shadow) "constructor-call")))
                    (succeeds  (= "class" (.toString (.getKind ?shadowParent))));we only want to show class - method/construct calls!
                    (equals ?pointcut (.getPointcut ?advice))
                    (equals ?fullyClassName (str (.getPackageName ?shadowParent)"."(.getName ?shadowParent)))
                    (equals ?aspectName (str "Aspect {"(.getSimpleName ?aspect)"}"))
                    (equals ?calledMth (str "In Class: " (.getName ?shadowParent)" -> "(.toString ?shadow)))
                    (equals ?adviceKind (str "Advice {"(.getKind ?advice)"}"))
                    (equals ?toLongStringmethod (first (clojure.string/split (first (rest (clojure.string/split (.getName ?shadow) #" "))) #"\(")))
                    (equals ?class (subs ?toLongStringmethod 0 (.lastIndexOf ?toLongStringmethod ".")))
                    (equals ?calledClass (str "<Class Name :" ?class ">"))
                    (equals ?methodName (subs ?toLongStringmethod (+ (.lastIndexOf ?toLongStringmethod ".") 1)))
                    (succeeds (= ?fullyClassName ?class))))
  
 (inspect (sort-by first  (ekeko [?CalledM  ?calledC  ?aspect ?adv ?pnt] (countPM ?calledC ?CalledM ?aspect ?adv ?pnt))))
 ;output ex: mobilemedia-> 
 ;["In Class: MediaListController -> method-call(void lancs.mobilemedia.core.ui.controller.MediaListController.appendMedias(lancs.mobilemedia.core.ui.datamodel.MediaData[], lancs.mobilemedia.core.ui.screens.MediaListScreen))" 
 ; "<Class Name :lancs.mobilemedia.core.ui.controller.MediaListController>" 
 ; "Aspect {SortingAspect}" 
 ; #<AdviceKind before> 
 ; #<AndPointcut (((call(public void lancs.mobilemedia.core.ui.controller.MediaListController.appendMedias(lancs.mobilemedia.core.ui.datamodel.MediaData[], lancs.mobilemedia.core.ui.screens.MediaListScreen)) && this(BindingTypePattern(lancs.mobilemedia.core.ui.controller.MediaListController, 0))) && args(BindingTypePattern(lancs.mobilemedia.core.ui.datamodel.MediaData[], 1), BindingTypePattern(lancs.mobilemedia.core.ui.screens.MediaListScreen, 2))) && persingleton(lancs.mobilemedia.optional.sorting.SortingAspect))>]
 ; The output says that a declared pointcut that connects with an advice before in SortingAspect refers to a method which is "appendMedias" in MediaListController
 ; so, this method is belongs to the class, in other words,a join point shadow matches the method called "appendMedias" of MediaListController  
 
 ;###############################* Coupling on intercepted modules (CIM) ###############################
 ;counts the number of modules completely named in the pointcuts in given aspects.
 (defn returnVectorForm-unfiltered [?shortAspect ?advicekind ?pointdefs ?items] 
                            (l/fresh [?aspect ?advice ?list]
                                     (NOAdvices ?aspect ?advice)
                                     (equals ?shortAspect (str "Aspect {"(.getSimpleName ?aspect)"}"))
                                     (equals ?pointdefs (.getPointcut ?advice))
                                     (equals ?advicekind (str "Advice {"(.toString (.getKind ?advice))"}"))
                                     (getRelatedKinds ?pointdefs ?list)
                                     (equals ?items (vec (into #{} ?list)))))
 
 (inspect (sort-by first (ekeko [?shortAspect ?advicekind ?pointdefs ?list] (returnVectorForm-unfiltered ?shortAspect ?advicekind ?pointdefs ?list))))
 
 ;consider only declaring type of fields, methods, and constructors , here we go...
 ;gathering the all primitive pointcuts that have a complete module name!! - (not look at the return type of eack item's name or items' name)
 (defn returnVectorForm-filtered [?shortAspect ?itemName ?advicekind ?pointdefs]
             (l/fresh [?list ?item ?decType ?excTypeName ?isInterface]
                        (returnVectorForm-unfiltered ?shortAspect ?advicekind ?pointdefs ?list)
                        (contains ?list ?item)
                        (equals ?itemName (str  (.getKind ?item)"{ "(.toString (.getSignature ?item))" }"))
                        (equals ?decType (.getDeclaringType (.getSignature ?item)))
                        (equals ?excTypeName (.getExactType ?decType))
                        (equals false (or 
                                        (IndexOfText (.toString ?decType) "+") ;eliminate all wildcards! that could be used to reach multiple modules in a project!
                                        (IndexOfText (.toString ?decType) "*")
                                        (IndexOfText (.toString ?decType) "..")))
                         (equals ?isInterface (getInterface (.toString ?excTypeName))); perhaps the type of the item is interface so, check it!
                         (succeeds (nil? ?isInterface))))
 
 (inspect (sort-by first (ekeko [?shortAspect ?item ?advicekind ?p] (returnVectorForm-filtered ?shortAspect ?item ?advicekind ?p ))))
 (count (ekeko [?shortAspect ?item ?nameaspect ?p] (returnVectorForm-filtered ?shortAspect ?item ?advicekind ?p )))
 ;for instance: method-call; the module name of the so-called "setPoolSize" is "db.ConnectionPool" in a given poincut connected with after around in the ConnectionPoolHandlers!
 ;[ "Aspect {ConnectionPoolHandlers}" 
 ;  "Advice {around}" 
 ;  "method-call{ * db.ConnectionPool.setPoolSize(..) }" 
 ;  #<OrPointcut ((((withincode(void db.ConnectionPool.setProperties(..)) && call(* db.ConnectionPool.setRestoreTimerDelay(..))) && persingleton(db.ConnectionPoolHandlers)) || ((withincode(void db.ConnectionPool.setProperties(..)) && call(* db.ConnectionPool.setPoolSize(..))) && persingleton(db.ConnectionPoolHandlers))) || (execution(void db.ConnectionPool.setProperties(..)) && persingleton(db.ConnectionPoolHandlers)))>]
 
 (defn getRelatedKinds [?pointcut ?list]
   "it is responsible for some primitive pointcuts that reach certain modules 
    such as method-call/execution, construct-call/execution, field-get/set"
   (l/fresh [?res ?y]
     (equals ?res (java.util.ArrayList. []))
     (equals ?y (calls ?pointcut ?res))
     (equals ?list ?res)))
 
 (defn calls [pointcut res]
   "a nested function \"calls\" is to pick up the related poincuts whose kind ID are 1!"
   (if  (or (= (getPKind pointcut) 5 ) (= (getPKind pointcut) 6))
      [(calls (getLeft pointcut) res) (calls (getRight pointcut) res)]
      (if (= (getPKind pointcut) 1) (addlist res pointcut))))
 
 ;############################### BasicAdvice : How many advice have a pointcut that uses more than the basics? ###############################
 (defn NOBAdvices [?total]
   (l/fresh [?advicesize ?advancesize ?aspect ?advice ?shortAspect ?nameaspect ?pointdefs ?list]
       (equals ?advicesize (count  (ekeko [?aspect ?advice] (NOAdvices ?aspect ?advice))))
       (equals ?advancesize (count (ekeko [?shortAspect ?nameaspect ?pointdefs ?list] (NOAAdvice ?shortAspect ?nameaspect ?pointdefs ?list))))
       (equals ?total (- ?advicesize ?advancesize))))
 
 (ekeko [?size ] (NOBAdvices ?size))
 
 ;###############################* AdvanceAdvice : How many advice depend on constructs that can only be determined at runtime? ###############################
 ; count NOAAdvice that have a poincut has at least one of the advance primitive pointcuts such as if, adviceexecution, cflow, and cflowbelow.
  (defn NOAAdvice [?shortAspect ?advicekind ?pointdefs ?ar]
                            (l/fresh [?aspect ?advice ?advices]
                                     (NOAdvices ?aspect ?advice)
                                     (equals ?shortAspect (str "Aspect {"(.getSimpleName ?aspect)"}"))
                                     (equals ?pointdefs (.getPointcut ?advice))
                                     (equals ?advicekind (str "Advice {"(.toString (.getKind ?advice))"}"))
                                     (getAdvance ?pointdefs ?advices)
                                     (equals ?ar (vec (into #{} ?advices)))
                                     (equals false (empty? ?ar))))
  
  (inspect  (sort-by first (ekeko [?shortAspect ?advicekind ?list ?pointdefs](NOAAdvice ?shortAspect ?advicekind ?pointdefs ?list))))
  (count (ekeko [?shortAspect ?advicekind ?list ?pointdefs] (NOAAdvice ?shortAspect ?advicekind ?pointdefs ?list)))
  (defn getAdvance [?pointcut ?arglist]
   "it is responsible for the primitive pointcut called \"getAdvance\" "
   (l/fresh [?result ?y]
     (equals ?result (java.util.ArrayList. []))
     (equals ?y (Advance ?pointcut ?result))
     (equals ?arglist ?result)))
 
 (defn Advance [pointcut res]
   "a nested function \"Advance\" is to pick up the related primitive poincuts!"
   (if  (or (= (getPKind pointcut) 5 ) (= (getPKind pointcut) 6))
      [(Advance (getLeft pointcut) res) (Advance (getRight pointcut) res)]
      (if (and (= (getPKind pointcut) 1) (= "adviceexecution" (.toString (getKind pointcut)))) (addlist res pointcut) 
         (if (or (= (getPKind pointcut) 9) (= (getPKind pointcut) 10)) (addlist res pointcut)))))
 
 ;############################### IAM : Do aspects often inherit from abstract aspects? ###############################
 (defn NOIAspects [?aspectname ?abstname]
         (l/fresh  [?aspect ?source ?super]
                   (NOAspects ?aspect ?source)
                   (w/aspect-declaredsuper ?aspect ?super)
                   (equals ?aspectname (str "Aspect {"(.getSimpleName ?aspect)"}"))
                   (equals ?abstname (str "From Abstract Aspect -> "(.getSimpleName ?super)))
                   (succeeds(.isAbstract ?super))))
 
 (inspect (sort-by first (ekeko [?aspectname ?name] (NOIAspects ?aspectname ?name))))
 (count (ekeko [?aspectname ?name] (NOIAspects ?aspectname ?name)))
 
 ;############################### NOSA: Are most aspects singletons? ###############################
 ; If an aspect has no parent aspect, then by default the aspect is a singleton aspect
  (defn NOSAspects [?aspectname]
         (l/fresh  [?source ?aspect ?super]
                   (NOAspects ?aspect ?source)
                   (w/aspect-declaredsuper ?aspect ?super)
                   (equals ?aspectname (str "Aspect {"(.getSimpleName ?aspect)"}"))
                   (succeeds (= "Object" (.getSimpleName ?super)))))
  
  (inspect (ekeko [?a] (NOSAspects ?a)))
  (count (ekeko [?a] (NOSAspects ?a))) 
  
 ;###############################* NOAE: How often is adviceExecution() used? ###############################
 (defn NOAdviceexecution [?shortAspect ?advicekind ?pointdefs ?arg] 
                            (l/fresh [?aspect ?advice ?args ?ar]
                                     (NOAdvices ?aspect ?advice)
                                     (equals ?shortAspect (str "Aspect {"(.getSimpleName ?aspect)"}"))
                                     (equals ?pointdefs (.getPointcut ?advice))
                                     (equals ?advicekind (str "Advice {"(.toString (.getKind ?advice))"}"))
                                     (getRelatedKinds ?pointdefs ?args)
                                     (equals ?ar (vec (into #{} ?args)))
                                     (contains ?ar ?arg)
                                     (equals true (= "adviceexecution" (.toString (getKind ?arg))))))
 
 (inspect  (sort-by first (ekeko [?shortAspect ?nameaspect ?list ?pointdefs] (NOAdviceexecution ?shortAspect ?nameaspect ?pointdefs ?list)))) 
 (count (ekeko [?shortAspect ?nameaspect ?list ?pointdefs]  (NOAdviceexecution ?shortAspect ?nameaspect ?pointdefs ?list)))
 
 ;############################### NOJPS: How many join point shadows per advice? ###############################
  (defn NOJPShadows [?aspectname ?adviceKind ?shadow]
                  (l/fresh [?aspect ?advice]
                           (NOAdvices ?aspect ?advice)
                           (w/advice-shadow ?advice ?shadow)
                           (equals ?aspectname (str "Aspect {"(.getSimpleName ?aspect)"}"))
                           (equals ?adviceKind (str "Advice {"(.getKind ?advice)"}"))))
  
  (inspect (sort-by first (ekeko [?a ?ad ?s] (NOJPShadows ?a ?ad ?s))))
  (count (ekeko [?a ?ad ?s] (NOJPShadows ?a ?ad ?s)))
  
 ;###############################* NOAG: How many args() are bound? ###############################
  (defn NOArgs [?shortAspect ?advicekind ?pointdefs ?arg] 
                            (l/fresh [?aspect ?advice ?args ?ar]
                                     (NOAdvices ?aspect ?advice)
                                     (equals ?shortAspect (str "Aspect {"(.getSimpleName ?aspect)"}"))
                                     (equals ?pointdefs (.getPointcut ?advice))
                                     (equals ?advicekind (str "Advice {"(.toString (.getKind ?advice))"}"))
                                     (getArgs ?pointdefs ?args)
                                     (equals ?ar (vec (into #{} ?args)))
                                     (contains ?ar ?arg)))
 
  (inspect  (sort-by first (ekeko [?shortAspect ?advicekind ?list ?pointdefs] (NOArgs ?shortAspect ?advicekind ?pointdefs ?list))))
  (count (ekeko [?shortAspect ?advicekind ?list ?pointdefs] (NOArgs ?shortAspect ?advicekind ?pointdefs ?list)))
  (defn getArgs [?pointcut ?arglist]
   "it is responsible for the primitive pointcut called \"getArgs\" "
   (l/fresh [?result ?y]
     (equals ?result (java.util.ArrayList. []))
     (equals ?y (PPargs ?pointcut ?result))
     (equals ?arglist ?result)))
 
 (defn PPargs [pointcut res]
   "a nested function \"PPargs\" is to pick up the related primitive poincut that its kind ID are 4!"
   (if  (or (= (getPKind pointcut) 5 ) (= (getPKind pointcut) 6))
      [(PPargs (getLeft pointcut) res) (PPargs (getRight pointcut) res)]
      (if (= (getPKind pointcut) 4) (addargs res pointcut))))
 
 ;############################### NOnPC: In how many around advice is it possible to (not) execute a proceed call ###############################
  (defn NOPC [?aspectName ?soot|methodName]; ?method : class soot.SootMethod
          (l/fresh [?aspect ?adv ?soot|advicemethod ?units ?unit]
            (NOAdvices ?aspect ?adv)
            (ajsoot/advice-soot|method ?adv ?soot|advicemethod)
            (succeeds (.hasActiveBody ?soot|advicemethod))
            (succeeds (.startsWith (.getName ?soot|advicemethod) "ajc$around$"))
            (soot|unit-getDeclarationClassname ?soot|advicemethod ?aspectName)
            (equals ?units (.getUnits (.getActiveBody ?soot|advicemethod)))
            (equals ?soot|methodName (str "Method {"(.getName ?soot|advicemethod)"}"))
            (contains ?units ?unit)
            (succeeds (.containsInvokeExpr ?unit))
	          (succeeds (or (= "soot.jimple.internal.JAssignStmt" (.getName (.getClass ?unit)))  
	                        (= "soot.jimple.internal.JInvokeStmt" (.getName (.getClass ?unit)))))
            (succeeds (and 
                        (.startsWith (.getName (.getMethod (.getValue (.getInvokeExprBox ?unit)))) "ajc")
                        (lastIndexOfText (.getName (.getMethod (.getValue (.getInvokeExprBox ?unit)))) "proceed")))))
 
 (inspect (sort-by first (ekeko [?a ?s] (NOPC ?a ?s))))
 (count (ekeko [?a ?s] (NOPC ?a ?s)))
 
 ;COUNT AROUND ADVICEs THAT HAVE NO PROCEED CALL
 (defn NOnPC []
    (- (countNOAround) (count (ekeko [?a ?s] (NOPC ?a ?s)))))
 (NOnPC)
 
 ;############################### How many before/after advice VS around advice? ###############################
 ;NOAround Advice
 (defn countNOAround []
   (count (ekeko [?a ?adv] 
                 (NOAdvices ?a ?adv)
                 (succeeds (= (.getName (.getKind ?adv)) "around")))))
 (countNOAround)
 
 (defn countNOAfter []
   (count (ekeko [?a ?adv] 
                 (NOAdvices ?a ?adv)
                 (succeeds (= (.getName (.getKind ?adv)) "after")))))
 (countNOAfter)
 
 (defn countNOBefore []
   (count (ekeko [?a ?adv] 
                 (NOAdvices ?a ?adv)
                 (succeeds (= (.getName (.getKind ?adv)) "before")))))
 (countNOBefore)

 ;Number of Before/After advices
 (+ (countNOAfter) (countNOBefore))
 
 ;############################### NOACsC: Do aspects often advise classes with a lot of subclasses?--how many advised classes that have subclasses? ###############################
 (defn NOClasses-executions [?classname]
   "collect all the advised classes -method/constructor execution"
   (l/fresh [?aspect ?advice ?shadow ?class]
            (NOAdvices ?aspect ?advice)
            (w/advice-shadow ?advice ?shadow)
            (succeeds (= "class" (.toString (.getKind (.getParent ?shadow)))))
            (equals ?class (.getParent ?shadow))
            (equals ?classname (str (.getPackageName (.getParent ?shadow))"."(.getName (.getParent ?shadow))))
            (equals false (nil? (getClass ?classname)))))
  
 (inspect (ekeko [?c] (NOClasses-executions ?c)))
  
 (defn NOClasses-calls [?classname ]
   "collect all the advised classes -method/constructor call"
   (l/fresh [?aspect ?advice ?shadow ?class]
           (NOAdvices ?aspect ?advice)
           (w/advice-shadow ?advice ?shadow)
           (succeeds (= "class" (.toString (.getKind (.getParent (.getParent ?shadow))))))
           (equals ?class (.getParent (.getParent ?shadow)))
           (equals ?classname (str (.getPackageName (.getParent ?shadow))"."(.getName (.getParent (.getParent ?shadow)))))
           (equals false (nil? (getClass ?classname)))))
 
 (defn getAdvisedClassesbyShadow []
   "combine the above two functions"
   (distinct
     (clojure.set/union
              (ekeko [?advisedc] (NOClasses-executions ?advisedc))
              (ekeko [?advisedc] (NOClasses-calls ?advisedc)))))
 (inspect (getAdvisedClassesbyShadow))
 
 (defn- getClassbyWeaver []
   "use distinct to eliminate duplicate classes that have subclasses"
   (distinct (ekeko [?c ] (SubclassesOfClasses ?c))))
 (inspect (getClassbyWeaver)) 
 
 ; Main function!
 ;let's check which advised class that has been collected, has subclass or subclasses
 (inspect (clojure.set/intersection (set (getClassbyWeaver)) (set(getAdvisedClassesbyShadow))))
 ; Main function! - this func takes a bit while! in order to return a result-
 (inspect  (ekeko [?gete ?as] 
                  (l/fresh [?get]
                           (equals ?get (getAdvisedClassesbyShadow))
                           (contains ?get ?gete)
                           (equals false (nil? (hasSubclass (first ?gete)))))))
 
 (defn- hasSubclass [?name] 
   (first (ekeko [?class]
                 (l/fresh [?fromfunc]
                          (equals ?fromfunc (getClassbyWeaver))
                          (contains ?fromfunc ?class)
                          (equals true (= (first ?class) ?name)))))) 
 ;(hasSubclass "org.jhotdraw.contrib.PolygonHandsle")
 
 (defn- SubclassesOfClasses [?class]
   "this function gets all the classes that have subclasses! by using the weaverworld! in a given project"
            (l/fresh [?subclass ?classes ?wclass ?wclassname]
                  (w/class ?classes)
                  (equals false (.isEnum ?classes))
                  (equals ?subclass (.getName ?classes))
                  (succeeds (empty? (filter #(re-matches #"\S+\$([1-9]|0)+" %) [(.getName ?classes)])))                  
                  (succeeds (IndexOfText (.getName ?classes) "$"))
                  (equals ?class (first (clojure.string/split ?subclass #"\$")))
                  (equals false (nil? (getClass ?class)))))

 (inspect (sort-by first (distinct (ekeko [?c ] (SubclassesOfClasses ?c)))))
 (defn- getClass [?name]
   (first
     (ekeko [?classes ?cn]
            (NOClasses ?classes)
            (equals ?cn (.getName ?classes))
            (equals true (= ?cn ?name))))) 
 
 ;############################### CE: How often is call used VS execution? ###############################
 ;method/construct call VS method/construct execution 
 (defn CE [?shortAspect ?advicekind  ?callexecution  ?pointdefs] 
                            (l/fresh [?aspect ?advice ?primPoint ?ar]
                                     (NOAdvices ?aspect ?advice)
                                     (equals ?shortAspect (str "Aspect {"(.getSimpleName ?aspect)"}"))
                                     (equals ?pointdefs (.getPointcut ?advice))
                                     (equals ?advicekind (str "Advice {"(.toString (.getKind ?advice))"}"))
                                     (getCallExe ?pointdefs ?primPoint)
                                     (equals ?ar (vec (into #{} ?primPoint)))
                                     (equals false (empty? ?ar))
                                     (contains ?ar ?callexecution)))
 
 (inspect  (sort-by first (ekeko [?shortAspect  ?advice ?list ?callexe] (CE ?shortAspect ?advice ?list ?callexe))))
 (count (ekeko [?shortAspect ?advice ?list ?callexe] (CE ?shortAspect ?advice  ?list ?callexe)))
 
 ;Number of CALL
 (defn NOCall [?call]
   (l/fresh [?shortAspect ?advicekind ?pointdefs]
            (CE ?shortAspect ?advicekind  ?call  ?pointdefs)
            (succeeds (or (= "method-call" (.toString (getKind ?call))) (= "constructor-call" (.toString (getKind ?call)))))))
 
 (inspect (ekeko [?c] (NOCall ?c)))
 (count (ekeko [?c] (NOCall ?c)))
 ;Number of EXECUTION
 (defn NOExecution [?execution]
   (l/fresh [?shortAspect ?advicekind ?pointdefs]
            (CE ?shortAspect ?advicekind  ?execution  ?pointdefs)
            (succeeds (or (= "method-execution" (.toString (getKind ?execution))) (= "constructor-execution" (.toString (getKind ?execution)))))))
 
 (inspect (sort-by first (ekeko [?s] (l/fresh [?e] (NOExecution ?e) (equals ?s (.toString ?e))))))
 (count (ekeko [?e] (NOExecution ?e)))
 
 ;-----------------------------------------------------------------------------
 (defn getCallExe [?pointcut ?arglist]
   "it is responsible for the primitive pointcut called \"getAdvance\" "
   (l/fresh [?result ?y]
     (equals ?result (java.util.ArrayList. []))
     (equals ?y (callVSexe ?pointcut ?result))
     (equals ?arglist ?result)))
 
(defn callVSexe [pointcut res]
   "a nested function \"Advance\" is to pick up the related primitive poincuts!"
   (if  (or (= (getPKind pointcut) 5 ) (= (getPKind pointcut) 6))
      [(callVSexe (getLeft pointcut) res) (callVSexe (getRight pointcut) res)]
      (if (and (= (getPKind pointcut) 1) (or (= "method-call"           (.toString (getKind pointcut)))
                                             (= "method-execution"      (.toString (getKind pointcut)))
                                             (= "constructor-call"      (.toString (getKind pointcut)))
                                             (= "constructor-execution" (.toString (getKind pointcut))))) (addlist res pointcut))))

 ;############################### NOnAC: Which parts of the system are (not) advised? Amount of non-advised VS advised methods ###############################
 ;Which parts of the system are advised? 
 (defn NOAClasses [?advised]
                  (l/fresh [?gets ?gete]
                           (equals ?gets (getAdvisedClassesbyShadow));line 739
                           (contains ?gets ?gete)
                           (equals ?advised (first ?gete))))
 (inspect (ekeko [?ac] (NOAClasses ?ac)))
 
 ;Main Function NOnonAdvisedClasses
 (defn NOnAClasses [] 
     (ekeko [?nonAdvised] 
            (l/fresh [?c] 
                     (NOClasses ?c) 
                     (equals ?nonAdvised (.getName ?c))
                     (succeeds (nil? (findtheclass ?nonAdvised))))))
 
 (inspect (NOnAClasses))
 (count (NOnAClasses)) 
 
 (defn- findtheclass [?name]
   (first (ekeko [?advc]
                 (NOAClasses ?advc)
                 (succeeds (= ?name ?advc)))))
 
 ;############################### NOW: How often are wildcards used in method/constructor-call/execution? ###############################
 (defn NOWilcards [?shortAspect ?itemName ?advicekind]
   (l/fresh [?callexecution ?isInterface ?pointdefs ?excTypeName ?decType]
            (CE ?shortAspect ?advicekind  ?callexecution ?pointdefs );line 790
            (equals ?itemName (str  (.getKind ?callexecution)"{ "(.toString (.getSignature ?callexecution))" }"))
            (equals ?decType (.getDeclaringType (.getSignature ?callexecution)))
            (equals ?excTypeName (.getExactType ?decType))
            (succeeds (or 
                        (IndexOfText (.toString ?decType) "+") ;check all wildcards!
                        (IndexOfText (.toString ?decType) "*")
                        (IndexOfText (.toString ?decType) "..")))
            (succeeds (nil? (getInterface (.toString ?excTypeName))))))
 
 (inspect (sort-by first (ekeko [?s ?i ?p] (NOWilcards ?s ?i ?p))))
 (count (ekeko [?s ?i ?p] (NOWilcards ?s ?i ?p)))

 ;############################### TJP: How often is thisjoinpoint used in a given AspectJ project? ###############################
 (defn NOThisJoinPoint [?DC|advicemethod ?sootname]
               (l/fresh [?soot|advicemethod ?adv ?aspect ?locals ?local]
                        (NOAdvices ?aspect ?adv);I am using this function or I can also use ajdt/advice function to get the proper declarded advice in a project and to avoid the errors!
                        (ajsoot/advice-soot|method ?adv ?soot|advicemethod)
                        (succeeds (.hasActiveBody ?soot|advicemethod))
                        (equals ?locals (.getLocals (.getActiveBody ?soot|advicemethod)))
                        (contains ?locals ?local)
                        (succeeds (= "org.aspectj.lang.JoinPoint" (.toString (.getType ?local))))
                        (equals ?sootname (.getName ?soot|advicemethod))
                        (equals ?DC|advicemethod (.getName (.getDeclaringClass ?soot|advicemethod)))))
 
 (inspect (sort-by first (ekeko [?a ?b] (NOThisJoinPoint ?a ?b))))
 (count (ekeko [?a ?b] (NOThisJoinPoint ?a ?b)))
 
 ;############################### NOPA: How often are args() only accessed? How often are they modified?- how many parameters do args(,,,) have?  ###############################
 ;Definition: the total number of the parameters of the entire args() primitive pointcut declared with advices of aspects in an aspectj project!
 (defn NOParametersofArgs  [?shortAspect ?advicekind ?pointdefs ?arg ?size]
   (l/fresh [?Argstr]
            (NOArgs ?shortAspect ?advicekind ?pointdefs ?arg )
            (equals ?Argstr (.toString ?arg))
            (equals false (or 
                            (IndexOfText ?Argstr "+") ;check all wildcards!
                            (IndexOfText ?Argstr "*")
                            (IndexOfText ?Argstr "..")))
            (equals ?size (.size (.getArguments ?arg)))))
 
 (inspect (ekeko [?shortAspect ?advicekind ?pointdefs ?arg ?size] (NOParametersofArgs ?shortAspect ?advicekind ?pointdefs ?arg ?size)))
 
 (defn- getSizeArgs [?size] 
         (l/fresh [?shortAspect ?advicekind ?pointdefs ?arg] 
                  (NOParametersofArgs ?shortAspect ?advicekind ?pointdefs ?arg ?size)))
 
 (defn totalcollection []  (ekeko [?s] (getSizeArgs ?s)))
 (totalcollection)
 
 ;)  
  (defn- addargs [lst selectedPointcut]
   (.add lst selectedPointcut)) 
  
  (defn- addlist [lst selectedPointcut]
   (.add lst selectedPointcut)) 
 
  (defn- getSignature [pointcut] 
      (.getSignature pointcut))
  
  (defn- getLeft [pointcut] 
      (.getLeft pointcut))
  
  (defn- getRight [pointcut] 
      (.getRight pointcut))
 
  (defn- getPKind [pointcut] 
      (.getPointcutKind pointcut))
  
  (defn- getKind [pointcut] 
      (.getKind pointcut))  
  
  (defn- getInterfaces-soot [?mn]
         (l/fresh [?m]
             (jsoot/soot :class ?m) 
             (equals ?mn (.getName ?m)) 
             (succeeds (.isInterface ?m))))
  
  ;(inspect (sort-by first (ekeko [?s] (getInterfaces-soot ?s))))
  
  (defn- getInterface-soot [?name] 
    (first (ekeko [?m]                   
                       (getInterfaces-soot ?m)
                       (equals true (= ?name ?m)))))
  
  ;(inspect (sort-by first (getInterface-soot "java.sql.ResultSet")))
  
  (defn- getInterface [?name]
    (first (ekeko [?i] 
         (w/interface ?i)
         (l/fresh [?nameI]
            (equals ?nameI (.getName ?i))
            (equals true 
                (or  (= ?name ?nameI) 
                     (= ?name "java.sql.Connection")
                     (= ?name "java.sql.PreparedStatement")
                     (= ?name "java.sql.ResultSet")
                     (= ?name "java.util.Collection")
                     (= ?name "java.util.Iterator")
                     (= ?name "javax.jms.QueueConnection")
                     (= ?name "javax.jms.TopicConnection")
                     (= ?name "javax.servlet.ServletContext")
                     (= ?name "javax.servlet.http.HttpServletRequest")
                     (= ?name "javax.servlet.http.HttpServletResponse")
                     (= ?name "javax.transaction.UserTransaction")
                     (= ?name "org.w3c.dom.Document")
                     (= ?name "org.w3c.dom.Element")
                     (= ?name "org.aspectj.lang.JoinPoint")
                     (= ?name "org.aspectj.lang.JoinPoint$StaticPart")
                     (= ?name "java.lang.annotation")
                     (= ?name "java.lang.Runnable")
                     (= ?name "java.util.Set")
                     (= ?name "java.sql.Statement")
                     (= ?name "java.util.Map")
                     (= ?name "javax.sql.DataSource")
                     (= ?name "java.sql.PreparedStatement")
                     (= ?name "javax.jms.TopicConnectionFactory")
                     (= ?name "javax.xml.transform.Source")
                     (= ?name "org.xml.sax.XMLReader")
                     (= ?name "javax.jms.QueueConnectionFactory")
                     (= ?name "java.rmi.Remote")))))))

  (defn- getEnum [?name]
        (first (ekeko [?i] (w/enum ?i) (equals true (= ?name (.getClassName ?i))))))
 
  (defn- 
   soot|unit-getDeclarationClassname  
   [?method ?decName]  
   (equals ?decName (str "Aspect {"(.getShortName (.getDeclaringClass ?method)) "}")))

 (defn- 
   soot|unit-getInvokeExprBoxMethod
   [?method]
    (.getMethod (.getValue (.getInvokeExprBox ?method))))

 (defn- 
   soot|unit-getInvokeExprMethod
   [?method]
   (.getMethod (.getInvokeExpr ?method)))

  (defn- 
    lastIndexOfText 
    [from to]  
    (> (.lastIndexOf from to) -1))

  (defn-  
    IndexOfText  
    [from to](> (.indexOf from to) -1))
